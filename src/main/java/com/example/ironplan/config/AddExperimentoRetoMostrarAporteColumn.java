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
 * Añade {@code experimento_reto.mostrar_aporte_equipo} antes de que Hibernate valide el
 * esquema ({@code ddl-auto=validate} en prod). Un {@code ApplicationRunner} llegaría
 * demasiado tarde: el contexto ni siquiera arranca si falta la columna.
 */
@Configuration
public class AddExperimentoRetoMostrarAporteColumn {

    private static final Logger log = LoggerFactory.getLogger(AddExperimentoRetoMostrarAporteColumn.class);

    @Bean
    public static BeanPostProcessor experimentoRetoMostrarAporteColumnPatch() {
        return new MostrarAporteColumnPatch();
    }

    private static final class MostrarAporteColumnPatch implements BeanPostProcessor, PriorityOrdered {
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
            if (!tableExists(st, "experimento_reto")) {
                log.warn("experimento_reto no existe aún; se omite mostrar_aporte_equipo");
                return;
            }
            if (columnExists(st, "experimento_reto", "mostrar_aporte_equipo")) {
                return;
            }
            st.execute("""
                    ALTER TABLE experimento_reto
                    ADD COLUMN mostrar_aporte_equipo BOOLEAN NOT NULL DEFAULT TRUE
                    """);
            log.info("Added experimento_reto.mostrar_aporte_equipo");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo agregar experimento_reto.mostrar_aporte_equipo", e);
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
