package com.example.ironplan.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.PriorityOrdered;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Añade {@code reto_activity_reviews.points_voided / voided_at / voided_by_user_id}
 * antes de que Hibernate valide el esquema ({@code ddl-auto=validate} en prod).
 * Idempotente: solo agrega lo que falta.
 */
@Configuration
public class AddRetoActivityReviewVoidedColumns {

    private static final Logger log = LoggerFactory.getLogger(AddRetoActivityReviewVoidedColumns.class);
    private static final String TABLE = "reto_activity_reviews";

    @Bean
    public static BeanPostProcessor retoActivityReviewVoidedColumnsPatch() {
        return new VoidedColumnsPatch();
    }

    private static final class VoidedColumnsPatch implements BeanPostProcessor, PriorityOrdered {
        private boolean applied;

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (!applied && bean instanceof DataSource ds) {
                apply(ds);
                applied = true;
            }
            return bean;
        }

        @Override
        public int getOrder() {
            // Después de CreateRetoActivityReviewsTable (HIGHEST_PRECEDENCE) para que la tabla ya exista.
            return HIGHEST_PRECEDENCE + 1;
        }
    }

    private static void apply(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            if (!tableExists(st, TABLE)) {
                log.warn("{} no existe aún; se omiten columnas de anulación", TABLE);
                return;
            }
            if (!columnExists(st, TABLE, "points_voided")) {
                st.execute("ALTER TABLE " + TABLE + " ADD COLUMN points_voided BOOLEAN NOT NULL DEFAULT FALSE");
                log.info("Added {}.points_voided", TABLE);
            }
            if (!columnExists(st, TABLE, "voided_at")) {
                st.execute("ALTER TABLE " + TABLE + " ADD COLUMN voided_at DATETIME(6) NULL");
                log.info("Added {}.voided_at", TABLE);
            }
            if (!columnExists(st, TABLE, "voided_by_user_id")) {
                String users = resolveTable(st, "Users", "users");
                st.execute("ALTER TABLE " + TABLE + " ADD COLUMN voided_by_user_id BIGINT NULL");
                if (users != null) {
                    st.execute("ALTER TABLE " + TABLE + " ADD CONSTRAINT FK_rar_voided_by "
                            + "FOREIGN KEY (voided_by_user_id) REFERENCES " + users + " (id)");
                }
                log.info("Added {}.voided_by_user_id", TABLE);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron agregar las columnas de anulación en " + TABLE, e);
        }
    }

    private static String resolveTable(Statement st, String... candidates) throws Exception {
        for (String name : candidates) {
            String sql = """
                    SELECT TABLE_NAME FROM information_schema.TABLES
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND LOWER(TABLE_NAME) = LOWER('%s')
                    LIMIT 1
                    """.formatted(name);
            try (ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private static boolean tableExists(Statement st, String table) throws Exception {
        String sql = """
                SELECT COUNT(*) FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = '%s'
                """.formatted(table);
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean columnExists(Statement st, String table, String column) throws Exception {
        String sql = """
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = '%s'
                  AND COLUMN_NAME = '%s'
                """.formatted(table, column);
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }
}
