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
 * Añade {@code organizational_groups.photo_url} antes de que Hibernate valide el
 * esquema ({@code ddl-auto=validate} en prod). Un {@code ApplicationRunner} llegaría
 * demasiado tarde: el contexto ni siquiera arranca si falta la columna.
 */
@Configuration
public class AddOrganizationalGroupPhotoUrlColumn {

    private static final Logger log = LoggerFactory.getLogger(AddOrganizationalGroupPhotoUrlColumn.class);

    @Bean
    public static BeanPostProcessor organizationalGroupPhotoUrlColumnPatch() {
        return new PhotoUrlColumnPatch();
    }

    private static final class PhotoUrlColumnPatch implements BeanPostProcessor, PriorityOrdered {
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
            if (!tableExists(st, "organizational_groups")) {
                log.warn("organizational_groups no existe aún; se omite photo_url");
                return;
            }
            if (columnExists(st, "organizational_groups", "photo_url")) {
                return;
            }
            st.execute("ALTER TABLE organizational_groups ADD COLUMN photo_url VARCHAR(500) NULL");
            log.info("Added organizational_groups.photo_url");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo agregar organizational_groups.photo_url", e);
        }
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
