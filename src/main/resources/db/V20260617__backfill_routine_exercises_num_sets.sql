-- Migración: backfill de num_sets tras renombrar columna "sets" (palabra reservada en MySQL).
-- Ejecutar manualmente en producción si routine_exercises.num_sets quedó en 0.

-- 1) Copiar desde la columna legacy "sets" si aún existe
UPDATE routine_exercises
SET num_sets = `sets`
WHERE (num_sets IS NULL OR num_sets = 0)
  AND `sets` IS NOT NULL
  AND `sets` > 0;

-- 2) Fallback para filas sin valor en ninguna columna
UPDATE routine_exercises
SET num_sets = 3
WHERE num_sets IS NULL OR num_sets = 0;

-- 3) (Opcional) Recalcular total_series por sesión de rutina
UPDATE routine_details rd
SET total_series = (
    SELECT COALESCE(SUM(re.num_sets), 0)
    FROM routine_exercises re
    WHERE re.session_id = rd.id
);
