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
 * Añade {@code experimento_reto.mostrar_posiciones} antes de que Hibernate valide el
 * esquema ({@code ddl-auto=validate} en prod). Si quedó la columna anterior
 * {@code mostrar_aporte_equipo}, la renombra.
 */
@Configuration
public class AddExperimentoRetoMostrarPosicionesColumn {

    private static final Logger log = LoggerFactory.getLogger(AddExperimentoRetoMostrarPosicionesColumn.class);

    @Bean
    public static BeanPostProcessor experimentoRetoMostrarPosicionesColumnPatch() {
        return new MostrarPosicionesColumnPatch();
    }

    private static final class MostrarPosicionesColumnPatch implements BeanPostProcessor, PriorityOrdered {
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
                log.warn("experimento_reto no existe aún; se omite mostrar_posiciones");
                return;
            }
            if (columnExists(st, "experimento_reto", "mostrar_posiciones")) {
                return;
            }
            if (columnExists(st, "experimento_reto", "mostrar_aporte_equipo")) {
                st.execute("""
                        ALTER TABLE experimento_reto
                        CHANGE mostrar_aporte_equipo mostrar_posiciones BOOLEAN NOT NULL DEFAULT TRUE
                        """);
                log.info("Renamed experimento_reto.mostrar_aporte_equipo to mostrar_posiciones");
                return;
            }
            st.execute("""
                    ALTER TABLE experimento_reto
                    ADD COLUMN mostrar_posiciones BOOLEAN NOT NULL DEFAULT TRUE
                    """);
            log.info("Added experimento_reto.mostrar_posiciones");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo agregar experimento_reto.mostrar_posiciones", e);
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
