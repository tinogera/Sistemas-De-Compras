-- T08.7: claves de idempotencia para crear solicitudes (header Idempotency-Key).
-- La PRIMARY KEY es lo que frena la carrera: si dos requests con la misma clave llegan a la vez,
-- el segundo en insertar falla y su transacción (con la solicitud incluida) se deshace.
CREATE TABLE solicitudes.clave_idempotencia (
    clave UUID PRIMARY KEY,
    solicitante_id BIGINT NOT NULL,
    solicitud_id BIGINT NOT NULL REFERENCES solicitudes.solicitud(id),
    creada_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
