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
 * Crea {@code reto_activity_reviews} antes de que Hibernate valide el esquema
 * ({@code ddl-auto=validate} en prod). Un {@code ApplicationRunner} llegaría
 * demasiado tarde: el contexto ni siquiera arranca si falta la tabla.
 */
@Configuration
public class CreateRetoActivityReviewsTable {

    private static final Logger log = LoggerFactory.getLogger(CreateRetoActivityReviewsTable.class);

    @Bean
    public static BeanPostProcessor retoActivityReviewsTablePatch() {
        return new TablePatch();
    }

    private static final class TablePatch implements BeanPostProcessor, PriorityOrdered {
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
            return HIGHEST_PRECEDENCE;
        }
    }

    private static void apply(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            if (tableExists(st, "reto_activity_reviews")) {
                return;
            }
            String users = resolveTable(st, "Users", "users");
            String competitions = resolveTable(st, "competitions", "Competitions");
            if (users == null || competitions == null) {
                log.warn("No se crea reto_activity_reviews: falta {} o {}", users, competitions);
                return;
            }
            st.execute("""
                    CREATE TABLE reto_activity_reviews (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        competition_id BIGINT NOT NULL,
                        user_id BIGINT NOT NULL,
                        source VARCHAR(32) NOT NULL,
                        source_id BIGINT NOT NULL,
                        flags VARCHAR(255) NULL,
                        note VARCHAR(1000) NULL,
                        status VARCHAR(32) NOT NULL,
                        marked_by_user_id BIGINT NULL,
                        marked_at DATETIME(6) NOT NULL,
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_reto_activity_review (competition_id, source, source_id),
                        KEY ix_rar_competition (competition_id),
                        CONSTRAINT FK_rar_competition FOREIGN KEY (competition_id) REFERENCES %s (id),
                        CONSTRAINT FK_rar_user FOREIGN KEY (user_id) REFERENCES %s (id),
                        CONSTRAINT FK_rar_marked_by FOREIGN KEY (marked_by_user_id) REFERENCES %s (id)
                    )
                    """.formatted(competitions, users, users));
            log.info("Created table reto_activity_reviews");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo crear reto_activity_reviews", e);
        }
    }

    private static String resolveTable(Statement st, String... candidates) throws Exception {
        for (String name : candidates) {
            String actual = actualTableName(st, name);
            if (actual != null) return actual;
        }
        return null;
    }

    private static String actualTableName(Statement st, String table) throws Exception {
        String sql = """
                SELECT TABLE_NAME FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND LOWER(TABLE_NAME) = LOWER('%s')
                LIMIT 1
                """.formatted(table);
        try (ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static boolean tableExists(Statement st, String table) throws Exception {
        return actualTableName(st, table) != null;
    }
}
