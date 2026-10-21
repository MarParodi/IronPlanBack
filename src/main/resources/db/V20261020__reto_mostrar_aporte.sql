-- Ocultar los lugares del reto (1.er, 2.º, 3.er). En prod ddl-auto=validate exige la columna al arrancar.
-- El deploy también la crea o renombra la columna anterior (AddExperimentoRetoMostrarPosicionesColumn).

ALTER TABLE experimento_reto
    ADD COLUMN mostrar_posiciones BOOLEAN NOT NULL DEFAULT TRUE;
