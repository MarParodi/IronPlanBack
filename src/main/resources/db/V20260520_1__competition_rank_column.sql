-- MySQL 8+: `rank` es palabra reservada; renombrar columna si existe con el nombre antiguo

SET @db := DATABASE();

SET @sql := (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE competition_participants CHANGE COLUMN `rank` position_rank INT NULL',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'competition_participants'
      AND COLUMN_NAME = 'rank'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE competition_member_participants CHANGE COLUMN `rank` position_rank INT NULL',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'competition_member_participants'
      AND COLUMN_NAME = 'rank'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
