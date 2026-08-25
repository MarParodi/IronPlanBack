package com.example.ironplan.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Completa el rename {@code sets} → {@code num_sets}. Hibernate {@code ddl-auto=update}
 * añade {@code num_sets} pero no elimina la columna legacy {@code sets} (NOT NULL sin default),
 * lo que hace fallar el INSERT al crear una rutina.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DropLegacyRoutineExerciseSetsColumn implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DropLegacyRoutineExerciseSetsColumn.class);

    private final DataSource dataSource;

    public DropLegacyRoutineExerciseSetsColumn(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            boolean hasSets = columnExists(st, "sets");
            boolean hasNumSets = columnExists(st, "num_sets");

            if (hasSets && !hasNumSets) {
                st.execute("ALTER TABLE routine_exercises RENAME COLUMN `sets` TO num_sets");
                log.info("Renamed routine_exercises.sets → num_sets");
                hasSets = false;
                hasNumSets = true;
            }

            if (hasSets && hasNumSets) {
                st.executeUpdate("""
                        UPDATE routine_exercises
                        SET num_sets = `sets`
                        WHERE (num_sets IS NULL OR num_sets = 0)
                          AND `sets` IS NOT NULL AND `sets` > 0
                        """);
                st.execute("ALTER TABLE routine_exercises DROP COLUMN `sets`");
                log.info("Dropped legacy routine_exercises.sets after backfill");
            }
        }
    }

    private static boolean columnExists(Statement st, String column) throws Exception {
        String sql = """
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'routine_exercises'
                  AND COLUMN_NAME = '%s'
                """.formatted(column);
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }
}
