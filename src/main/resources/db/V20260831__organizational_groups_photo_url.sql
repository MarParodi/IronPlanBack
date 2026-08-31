-- Foto de grupo (URL). En prod ddl-auto=validate exige la columna al arrancar.
-- El deploy también la crea solo si falta (AddOrganizationalGroupPhotoUrlColumn).

ALTER TABLE organizational_groups
    ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500) NULL;
