CREATE TABLE IF NOT EXISTS reto_activity_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    source VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    flags VARCHAR(255) NULL,
    note VARCHAR(1000) NULL,
    status VARCHAR(32) NOT NULL,
    marked_by_user_id BIGINT NULL,
    marked_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reto_activity_review (competition_id, source, source_id),
    KEY ix_rar_competition (competition_id),
    CONSTRAINT FK_rar_competition FOREIGN KEY (competition_id) REFERENCES competitions (id),
    CONSTRAINT FK_rar_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT FK_rar_marked_by FOREIGN KEY (marked_by_user_id) REFERENCES users (id)
);
