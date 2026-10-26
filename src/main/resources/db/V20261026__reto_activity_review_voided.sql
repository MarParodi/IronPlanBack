-- Anulación de puntos por actividad desde la vista administrativa del reto.
-- En prod ddl-auto=validate exige las columnas al arrancar; el deploy también
-- las crea solo si faltan (AddRetoActivityReviewVoidedColumns).

ALTER TABLE reto_activity_reviews
    ADD COLUMN points_voided BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN voided_at DATETIME(6) NULL,
    ADD COLUMN voided_by_user_id BIGINT NULL,
    ADD CONSTRAINT FK_rar_voided_by FOREIGN KEY (voided_by_user_id) REFERENCES users (id);
