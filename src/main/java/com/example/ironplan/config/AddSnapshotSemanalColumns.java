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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Añade las variables del experimento a {@code snapshot_semanal_usuario} antes de que
 * Hibernate valide el esquema ({@code ddl-auto=validate} en prod). Un {@code ApplicationRunner}
 * llegaría demasiado tarde: el contexto ni siquiera arranca si faltan las columnas.
 */
@Configuration
public class AddSnapshotSemanalColumns {

    private static final Logger log = LoggerFactory.getLogger(AddSnapshotSemanalColumns.class);

    private static final String TABLE = "snapshot_semanal_usuario";

    private static final Map<String, String> COLUMNS = new LinkedHashMap<>();
    static {
        COLUMNS.put("sesiones_totales", "INT NOT NULL DEFAULT 0");
        COLUMNS.put("sesiones_previstas", "DECIMAL(6,2) NULL");
        COLUMNS.put("adherencia_pct", "DECIMAL(7,2) NULL");
        COLUMNS.put("dias_activos", "INT NOT NULL DEFAULT 0");
        COLUMNS.put("minutos_fuerza", "INT NOT NULL DEFAULT 0");
        COLUMNS.put("carga_promedio", "DECIMAL(8,2) NULL");
        COLUMNS.put("xp_acumulado_periodo", "INT NOT NULL DEFAULT 0");
        COLUMNS.put("rango_fin_semana", "VARCHAR(30) NULL");
        COLUMNS.put("logros_semana", "INT NOT NULL DEFAULT 0");
        COLUMNS.put("logros_acumulados", "INT NOT NULL DEFAULT 0");
        COLUMNS.put("puntos_reto_semana", "DECIMAL(10,2) NULL");
        COLUMNS.put("puntos_reto_acumulados", "DECIMAL(10,2) NULL");
        COLUMNS.put("semana_completa", "BOOLEAN NOT NULL DEFAULT FALSE");
        COLUMNS.put("activo", "BOOLEAN NOT NULL DEFAULT TRUE");
    }

    @Bean
    public static BeanPostProcessor snapshotSemanalColumnsPatch() {
        return new ColumnsPatch();
    }

    private static final class ColumnsPatch implements BeanPostProcessor, PriorityOrdered {
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
            if (table == null) {
                log.warn("{} no existe aún; se omiten sus columnas nuevas", TABLE);
                return;
            }
            for (var entry : COLUMNS.entrySet()) {
                if (columnExists(st, table, entry.getKey())) continue;
                st.execute("ALTER TABLE " + table + " ADD COLUMN " + entry.getKey() + " " + entry.getValue());
                log.info("Added {}.{}", table, entry.getKey());
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron agregar las columnas de " + TABLE, e);
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
