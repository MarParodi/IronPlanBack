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
 * Convierte {@code free_activity_sessions.activity_type} de ENUM nativo de MySQL a VARCHAR
 * antes de que Hibernate valide el esquema, para que los tipos nuevos de
 * {@link com.example.ironplan.model.FreeActivityType} no requieran alterar la tabla.
 */
@Configuration
public class FreeActivityTypeColumnToVarchar {

    private static final Logger log = LoggerFactory.getLogger(FreeActivityTypeColumnToVarchar.class);

    private static final String TABLE = "free_activity_sessions";
    private static final String COLUMN = "activity_type";

    @Bean
    public static BeanPostProcessor freeActivityTypeColumnPatch() {
        return new ColumnPatch();
    }

    private static final class ColumnPatch implements BeanPostProcessor, PriorityOrdered {
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
            String table = actualTableName(st, TABLE);
            if (table == null) return;
            if (!"enum".equalsIgnoreCase(columnDataType(st, table, COLUMN))) return;
            st.execute("ALTER TABLE " + table + " MODIFY COLUMN " + COLUMN + " VARCHAR(40) NOT NULL");
            log.info("Converted {}.{} from ENUM to VARCHAR(40)", table, COLUMN);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo convertir " + TABLE + "." + COLUMN + " a VARCHAR", e);
        }
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

    private static String columnDataType(Statement st, String table, String column) throws Exception {
        String sql = """
                SELECT DATA_TYPE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = '%s'
                  AND COLUMN_NAME = '%s'
                """.formatted(table, column);
        try (ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }
}
