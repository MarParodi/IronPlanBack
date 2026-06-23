CREATE TABLE IF NOT EXISTS competition_podium_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    scope VARCHAR(20) NOT NULL,
    level_category VARCHAR(20) NULL,
    rank_position INT NOT NULL,
    composite_score DOUBLE NOT NULL DEFAULT 0,
    consistency_raw DOUBLE NOT NULL DEFAULT 0,
    one_rm_progress_raw DOUBLE NOT NULL DEFAULT 0,
    volume_raw DOUBLE NOT NULL DEFAULT 0,
    consistency_norm DOUBLE NOT NULL DEFAULT 0,
    one_rm_norm DOUBLE NOT NULL DEFAULT 0,
    volume_norm DOUBLE NOT NULL DEFAULT 0,
    generated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_podium_rank (competition_id, scope, level_category, rank_position),
    CONSTRAINT FK_cpe_competition FOREIGN KEY (competition_id) REFERENCES competitions (id),
    CONSTRAINT FK_cpe_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS competition_declared_winner (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    scope VARCHAR(20) NOT NULL,
    level_category VARCHAR(20) NULL,
    declared_at DATETIME(6) NOT NULL,
    declared_by_user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_declared_winner (competition_id, scope, level_category),
    CONSTRAINT FK_cdw_competition FOREIGN KEY (competition_id) REFERENCES competitions (id),
    CONSTRAINT FK_cdw_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT FK_cdw_declared_by FOREIGN KEY (declared_by_user_id) REFERENCES users (id)
);
