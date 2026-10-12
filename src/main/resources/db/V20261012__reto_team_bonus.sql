-- Bonos manuales por equipo en retos TEAM_POINTS. En prod ddl-auto=validate exige la tabla al arrancar.
-- El deploy también la crea solo si falta (CreateRetoTeamBonusTable).

CREATE TABLE IF NOT EXISTS reto_team_bonus (
    id BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    organizational_group_id BIGINT NOT NULL,
    points DOUBLE NOT NULL,
    title VARCHAR(150) NOT NULL,
    evidence_url VARCHAR(500) NULL,
    evidence_public_id VARCHAR(255) NULL,
    created_by_user_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_rtb_competition_group (competition_id, organizational_group_id),
    CONSTRAINT FK_rtb_competition FOREIGN KEY (competition_id) REFERENCES competitions (id),
    CONSTRAINT FK_rtb_group FOREIGN KEY (organizational_group_id) REFERENCES organizational_groups (id),
    CONSTRAINT FK_rtb_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);
