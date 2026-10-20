-- Ocultar el aporte de puntos del participante. En prod ddl-auto=validate exige la columna al arrancar.
-- El deploy también la crea solo si falta (AddExperimentoRetoMostrarAporteColumn).

ALTER TABLE experimento_reto
    ADD COLUMN mostrar_aporte_equipo BOOLEAN NOT NULL DEFAULT TRUE;
