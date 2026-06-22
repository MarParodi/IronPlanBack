-- Ampliar metric_type para actividad libre (FREE_ACTIVITY_COUNT, FREE_ACTIVITY_KM).
-- MySQL ENUM legacy truncaba valores nuevos → error 500 al registrar actividad libre.
-- Ejecutar en local y en Railway (MySQL) si ddl-auto=update no alteró la columna.

ALTER TABLE user_activities
    MODIFY COLUMN metric_type VARCHAR(32) NOT NULL;

ALTER TABLE competitions
    MODIFY COLUMN metric_type VARCHAR(32) NOT NULL;
