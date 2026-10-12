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
 * Crea {@code reto_team_bonus} antes de que Hibernate valide el esquema
 * ({@code ddl-auto=validate} en prod). Un {@code ApplicationRunner} llegaría
 * demasiado tarde: el contexto ni siquiera arranca si falta la tabla.
 */
@Configuration
public class CreateRetoTeamBonusTable {

    private static final Logger log = LoggerFactory.getLogger(CreateRetoTeamBonusTable.class);

    @Bean
    public static BeanPostProcessor retoTeamBonusTablePatch() {
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
            if (tableExists(st, "reto_team_bonus")) {
                return;
            }
            String users = resolveTable(st, "Users", "users");
            String competitions = resolveTable(st, "competitions", "Competitions");
            String groups = resolveTable(st, "organizational_groups", "Organizational_Groups");
            if (users == null || competitions == null || groups == null) {
                log.warn("No se crea reto_team_bonus: falta {}, {} o {}", users, competitions, groups);
                return;
            }
            st.execute("""
                    CREATE TABLE reto_team_bonus (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        competition_id BIGINT NOT NULL,
                        organizational_group_id BIGINT NOT NULL,
                        points DOUBLE NOT NULL,
                        title VARCHAR(150) NOT NULL,
                        evidence_url VARCHAR(500) NULL,
                        evidence_public_id VARCHAR(255) NULL,
                        created_by_user_id BIGINT NULL,
                        created_at DATETIME(6) NOT NULL,
                        PRIMARY KEY (id),
                        KEY ix_rtb_competition_group (competition_id, organizational_group_id),
                        CONSTRAINT FK_rtb_competition FOREIGN KEY (competition_id) REFERENCES %s (id),
                        CONSTRAINT FK_rtb_group FOREIGN KEY (organizational_group_id) REFERENCES %s (id),
                        CONSTRAINT FK_rtb_created_by FOREIGN KEY (created_by_user_id) REFERENCES %s (id)
                    )
                    """.formatted(competitions, groups, users));
            log.info("Created table reto_team_bonus");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo crear reto_team_bonus", e);
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
