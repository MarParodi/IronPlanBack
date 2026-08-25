-- Completar rename sets → num_sets (sets es palabra reservada en MySQL).
-- Ejecutar en local y en Railway (MySQL) si ddl-auto=update dejó la columna legacy `sets`.
-- Idempotente: no falla si `sets` ya no existe.
--
-- Verificar:
--   SHOW COLUMNS FROM routine_exercises LIKE '%set%';
--
-- Casos:
--   sets + num_sets → backfill y DROP COLUMN `sets`
--   solo sets       → RENAME COLUMN `sets` TO num_sets
--   solo num_sets   → no-op

SET @db := DATABASE();

SET @has_sets := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'routine_exercises'
      AND COLUMN_NAME = 'sets'
);

SET @has_num_sets := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'routine_exercises'
      AND COLUMN_NAME = 'num_sets'
);

-- Caso B: solo existe `sets` → renombrar
SET @sql := IF(
    @has_sets = 1 AND @has_num_sets = 0,
    'ALTER TABLE routine_exercises RENAME COLUMN `sets` TO num_sets',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Caso A: coexisten ambas → copiar valores legacy
SET @sql := IF(
    @has_sets = 1 AND @has_num_sets = 1,
    'UPDATE routine_exercises SET num_sets = `sets` WHERE (num_sets IS NULL OR num_sets = 0) AND `sets` IS NOT NULL AND `sets` > 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Fallback para filas sin valor
UPDATE routine_exercises
SET num_sets = 3
WHERE num_sets IS NULL OR num_sets = 0;

-- Recalcular total_series por sesión
UPDATE routine_sessions rs
SET total_series = (
    SELECT COALESCE(SUM(re.num_sets), 0)
    FROM routine_exercises re
    WHERE re.session_id = rs.id
);

-- Releer por si el RENAME ya eliminó `sets`
SET @has_sets := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'routine_exercises'
      AND COLUMN_NAME = 'sets'
);

SET @has_num_sets := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'routine_exercises'
      AND COLUMN_NAME = 'num_sets'
);

-- Caso A: eliminar la columna legacy (Hibernate ddl-auto=update no la borra)
SET @sql := IF(
    @has_sets = 1 AND @has_num_sets = 1,
    'ALTER TABLE routine_exercises DROP COLUMN `sets`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
