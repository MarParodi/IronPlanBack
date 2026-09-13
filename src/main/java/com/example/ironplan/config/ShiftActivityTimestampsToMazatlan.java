package com.example.ironplan.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.PriorityOrdered;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Pasa {@code started_at}/{@code completed_at} de UTC naive a hora de Los Mochis (UTC−7)
 * una sola vez, antes de aceptar tráfico. El puntaje del reto se recalcula desde esas columnas.
 */
@Configuration
@ConditionalOnProperty(name = "app.timezone.migrate-utc-to-mazatlan", havingValue = "true")
public class ShiftActivityTimestampsToMazatlan {

    private static final Logger log = LoggerFactory.getLogger(ShiftActivityTimestampsToMazatlan.class);
    static final String PATCH_NAME = "utc_to_mazatlan_minus_7h_v1";

    @Bean
    public static BeanPostProcessor activityTimestampsMazatlanPatch() {
        return new TimestampPatch();
    }

    private static final class TimestampPatch implements BeanPostProcessor, PriorityOrdered {
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
        try (Connection conn = dataSource.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                ensurePatchesTable(st);
                if (patchApplied(st)) {
                    conn.rollback();
                    return;
                }

                shiftTable(st, "free_activity_sessions",
                        "started_at = TIMESTAMPADD(HOUR, -7, started_at), "
                                + "completed_at = TIMESTAMPADD(HOUR, -7, completed_at), "
                                + "created_at = TIMESTAMPADD(HOUR, -7, created_at)");
                shiftTable(st, "workout_sessions",
                        "started_at = TIMESTAMPADD(HOUR, -7, started_at), "
                                + "completed_at = TIMESTAMPADD(HOUR, -7, completed_at), "
                                + "created_at = TIMESTAMPADD(HOUR, -7, created_at), "
                                + "updated_at = IF(updated_at IS NULL, NULL, TIMESTAMPADD(HOUR, -7, updated_at))");
                shiftTable(st, "workout_exercises",
                        "started_at = IF(started_at IS NULL, NULL, TIMESTAMPADD(HOUR, -7, started_at)), "
                                + "finished_at = IF(finished_at IS NULL, NULL, TIMESTAMPADD(HOUR, -7, finished_at))");

                realignActivityDates(st);
                st.executeUpdate("""
                        INSERT INTO ironplan_data_patches (patch_name, applied_at)
                        VALUES ('%s', CURRENT_TIMESTAMP(6))
                        """.formatted(PATCH_NAME));
                conn.commit();
                log.info("Aplicado parche {}: timestamps de actividad/fuerza UTC → America/Mazatlan", PATCH_NAME);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron convertir timestamps a horario de Los Mochis", e);
        }
    }

    private static void ensurePatchesTable(Statement st) throws Exception {
        st.execute("""
                CREATE TABLE IF NOT EXISTS ironplan_data_patches (
                    patch_name VARCHAR(120) NOT NULL PRIMARY KEY,
                    applied_at DATETIME(6) NOT NULL
                )
                """);
    }

    private static boolean patchApplied(Statement st) throws Exception {
        try (ResultSet rs = st.executeQuery(
                "SELECT 1 FROM ironplan_data_patches WHERE patch_name = '" + PATCH_NAME + "' LIMIT 1")) {
            return rs.next();
        }
    }

    private static void shiftTable(Statement st, String table, String setClause) throws Exception {
        String actual = actualTableName(st, table);
        if (actual == null) {
            log.warn("Se omite {}: la tabla no existe", table);
            return;
        }
        int updated = st.executeUpdate("UPDATE " + actual + " SET " + setClause);
        log.info("Parche {}: {} filas en {}", PATCH_NAME, updated, actual);
    }

    private static void realignActivityDates(Statement st) throws Exception {
        String activities = actualTableName(st, "user_activities");
        String libres = actualTableName(st, "free_activity_sessions");
        String workouts = actualTableName(st, "workout_sessions");
        if (activities == null) {
            return;
        }
        if (libres != null) {
            st.executeUpdate("""
                    UPDATE %s ua
                    INNER JOIN %s f ON f.id = ua.source_id AND f.user_id = ua.user_id
                    SET ua.activity_date = DATE(f.completed_at)
                    WHERE ua.metric_type IN ('FREE_ACTIVITY_COUNT', 'FREE_ACTIVITY_KM')
                    """.formatted(activities, libres));
        }
        if (workouts != null) {
            st.executeUpdate("""
                    UPDATE %s ua
                    INNER JOIN %s w ON w.id = ua.source_id AND w.user_id = ua.user_id
                    SET ua.activity_date = DATE(COALESCE(w.completed_at, w.started_at))
                    WHERE ua.metric_type = 'VOLUME_TOTAL'
                    """.formatted(activities, workouts));
        }
        if (libres != null && workouts != null) {
            st.executeUpdate("""
                    UPDATE %s ua
                    INNER JOIN %s f ON f.id = ua.source_id AND f.user_id = ua.user_id
                    SET ua.activity_date = DATE(f.completed_at)
                    WHERE ua.metric_type IN ('SESSIONS', 'WORKOUTS_COUNT', 'ACTIVE_MINUTES')
                      AND NOT EXISTS (
                          SELECT 1 FROM %s w
                          WHERE w.id = ua.source_id AND w.user_id = ua.user_id
                      )
                    """.formatted(activities, libres, workouts));
            st.executeUpdate("""
                    UPDATE %s ua
                    INNER JOIN %s w ON w.id = ua.source_id AND w.user_id = ua.user_id
                    SET ua.activity_date = DATE(COALESCE(w.completed_at, w.started_at))
                    WHERE ua.metric_type IN ('SESSIONS', 'WORKOUTS_COUNT', 'ACTIVE_MINUTES')
                      AND NOT EXISTS (
                          SELECT 1 FROM %s f
                          WHERE f.id = ua.source_id AND f.user_id = ua.user_id
                      )
                    """.formatted(activities, workouts, libres));
        } else if (libres != null) {
            st.executeUpdate("""
                    UPDATE %s ua
                    INNER JOIN %s f ON f.id = ua.source_id AND f.user_id = ua.user_id
                    SET ua.activity_date = DATE(f.completed_at)
                    WHERE ua.metric_type IN ('SESSIONS', 'WORKOUTS_COUNT', 'ACTIVE_MINUTES')
                    """.formatted(activities, libres));
        } else if (workouts != null) {
            st.executeUpdate("""
                    UPDATE %s ua
                    INNER JOIN %s w ON w.id = ua.source_id AND w.user_id = ua.user_id
                    SET ua.activity_date = DATE(COALESCE(w.completed_at, w.started_at))
                    WHERE ua.metric_type IN ('SESSIONS', 'WORKOUTS_COUNT', 'ACTIVE_MINUTES')
                    """.formatted(activities, workouts));
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
}
