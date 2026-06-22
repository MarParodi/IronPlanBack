-- Experimento de tesis + columnas de telemetría en workout_sets
-- Ejecutar en local/Railway si ddl-auto=update no aplica todo.

ALTER TABLE workout_sets
    ADD COLUMN IF NOT EXISTS rir_registrado TINYINT NULL,
    ADD COLUMN IF NOT EXISTS one_rm_estimado DECIMAL(6,2) NULL,
    ADD COLUMN IF NOT EXISTS volumen_serie DECIMAL(10,2) NULL;

CREATE TABLE IF NOT EXISTS experimento_reto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    organizacion_id BIGINT NOT NULL,
    competition_id BIGINT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    semanas_intervencion INT NOT NULL DEFAULT 8,
    estado VARCHAR(20) NOT NULL DEFAULT 'PLANEACION',
    ambito VARCHAR(20) NOT NULL DEFAULT 'ORGANIZACION',
    posttest_ipaq_activo BOOLEAN NOT NULL DEFAULT FALSE,
    sus_activo BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_experimento_org FOREIGN KEY (organizacion_id) REFERENCES organizational_groups(id),
    CONSTRAINT fk_experimento_comp FOREIGN KEY (competition_id) REFERENCES competitions(id)
);

CREATE TABLE IF NOT EXISTS participante_reto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reto_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    categoria VARCHAR(20) NOT NULL,
    objetivo_codigo VARCHAR(10) NOT NULL,
    objetivo_texto_libre TEXT,
    fecha_inscripcion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completo_pretest BOOLEAN NOT NULL DEFAULT FALSE,
    completo_posttest BOOLEAN NOT NULL DEFAULT FALSE,
    completo_sus BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_participante_reto (reto_id, usuario_id),
    CONSTRAINT fk_participante_reto FOREIGN KEY (reto_id) REFERENCES experimento_reto(id),
    CONSTRAINT fk_participante_usuario FOREIGN KEY (usuario_id) REFERENCES Users(id)
);

CREATE TABLE IF NOT EXISTS ipaq_respuesta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    participante_reto_id BIGINT NOT NULL,
    corte VARCHAR(10) NOT NULL,
    fecha_aplicacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    caminata_dias_semana TINYINT,
    caminata_min_dia SMALLINT,
    moderada_dias_semana TINYINT,
    moderada_min_dia SMALLINT,
    vigorosa_dias_semana TINYINT,
    vigorosa_min_dia SMALLINT,
    met_caminata DECIMAL(10,2),
    met_moderada DECIMAL(10,2),
    met_vigorosa DECIMAL(10,2),
    met_total_semana DECIMAL(10,2),
    categoria_ipaq VARCHAR(20),
    es_outlier BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_ipaq_corte (participante_reto_id, corte),
    CONSTRAINT fk_ipaq_participante FOREIGN KEY (participante_reto_id) REFERENCES participante_reto(id)
);

CREATE TABLE IF NOT EXISTS sus_respuesta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    participante_reto_id BIGINT NOT NULL,
    fecha_aplicacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sus_q1 TINYINT NOT NULL,
    sus_q2 TINYINT NOT NULL,
    sus_q3 TINYINT NOT NULL,
    sus_q4 TINYINT NOT NULL,
    sus_q5 TINYINT NOT NULL,
    sus_q6 TINYINT NOT NULL,
    sus_q7 TINYINT NOT NULL,
    sus_q8 TINYINT NOT NULL,
    sus_q9 TINYINT NOT NULL,
    sus_q10 TINYINT NOT NULL,
    puntaje_sus DECIMAL(5,2),
    UNIQUE KEY uk_sus_participante (participante_reto_id),
    CONSTRAINT fk_sus_participante FOREIGN KEY (participante_reto_id) REFERENCES participante_reto(id)
);

CREATE TABLE IF NOT EXISTS consentimiento_informado (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    participante_reto_id BIGINT NOT NULL,
    fecha_aceptacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_dispositivo VARCHAR(45),
    version_documento VARCHAR(10) DEFAULT '1.0',
    acepto BOOLEAN NOT NULL,
    UNIQUE KEY uk_consentimiento (participante_reto_id),
    CONSTRAINT fk_consentimiento_participante FOREIGN KEY (participante_reto_id) REFERENCES participante_reto(id)
);

CREATE TABLE IF NOT EXISTS snapshot_semanal_usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reto_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    numero_semana TINYINT NOT NULL,
    fecha_inicio_semana DATE NOT NULL,
    fecha_fin_semana DATE NOT NULL,
    sesiones_completadas SMALLINT NOT NULL DEFAULT 0,
    volumen_total_semana DECIMAL(12,2) NOT NULL DEFAULT 0,
    one_rm_promedio DECIMAL(6,2),
    one_rm_maximo DECIMAL(6,2),
    xp_acumulado_al_fin INT NOT NULL DEFAULT 0,
    xp_ganado_semana INT NOT NULL DEFAULT 0,
    posicion_leaderboard SMALLINT,
    sesiones_cardio SMALLINT NOT NULL DEFAULT 0,
    minutos_cardio INT NOT NULL DEFAULT 0,
    km_cardio DECIMAL(8,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_snapshot_semana (reto_id, usuario_id, numero_semana),
    CONSTRAINT fk_snapshot_reto FOREIGN KEY (reto_id) REFERENCES experimento_reto(id),
    CONSTRAINT fk_snapshot_usuario FOREIGN KEY (usuario_id) REFERENCES Users(id)
);
