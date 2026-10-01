-- Variables del experimento en snapshots semanales. En prod ddl-auto=validate exige las columnas al arrancar.
-- El deploy también las crea solo si faltan (AddSnapshotSemanalColumns).

ALTER TABLE snapshot_semanal_usuario
    ADD COLUMN IF NOT EXISTS sesiones_totales INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sesiones_previstas DECIMAL(6,2) NULL,
    ADD COLUMN IF NOT EXISTS adherencia_pct DECIMAL(7,2) NULL,
    ADD COLUMN IF NOT EXISTS dias_activos INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS minutos_fuerza INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS carga_promedio DECIMAL(8,2) NULL,
    ADD COLUMN IF NOT EXISTS xp_acumulado_periodo INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rango_fin_semana VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS logros_semana INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS logros_acumulados INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS puntos_reto_semana DECIMAL(10,2) NULL,
    ADD COLUMN IF NOT EXISTS puntos_reto_acumulados DECIMAL(10,2) NULL,
    ADD COLUMN IF NOT EXISTS semana_completa BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
