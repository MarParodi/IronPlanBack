-- Tablas de participantes en competencias (requeridas por Competition / GruposService)

CREATE TABLE IF NOT EXISTS competition_participants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    organizational_group_id BIGINT NOT NULL,
    group_score DOUBLE NOT NULL DEFAULT 0,
    position_rank INT NULL,
    last_calculated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UK_competition_group (competition_id, organizational_group_id),
    CONSTRAINT FK_cp_competition FOREIGN KEY (competition_id) REFERENCES competitions (id),
    CONSTRAINT FK_cp_org_group FOREIGN KEY (organizational_group_id) REFERENCES organizational_groups (id)
);

CREATE TABLE IF NOT EXISTS competition_member_participants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    score DOUBLE NOT NULL DEFAULT 0,
    position_rank INT NULL,
    last_calculated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UK_comp_member (competition_id, user_id),
    CONSTRAINT FK_cmp_competition FOREIGN KEY (competition_id) REFERENCES competitions (id),
    CONSTRAINT FK_cmp_user FOREIGN KEY (user_id) REFERENCES users (id)
);
