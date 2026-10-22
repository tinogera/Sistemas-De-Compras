ALTER TABLE solicitudes.solicitud
    ALTER COLUMN solicitante_id TYPE BIGINT USING solicitante_id::BIGINT,
    ALTER COLUMN sector_id TYPE BIGINT USING sector_id::BIGINT;

ALTER TABLE solicitudes.item
    ALTER COLUMN categoria_id TYPE BIGINT USING categoria_id::BIGINT;
