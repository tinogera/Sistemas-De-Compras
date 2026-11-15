CREATE SEQUENCE ordenes.numero_orden_seq;

CREATE TABLE ordenes.orden_pedido (
    id BIGSERIAL PRIMARY KEY,
    numero BIGINT NOT NULL UNIQUE
      DEFAULT nextval('ordenes.numero_orden_seq'),
    item_id BIGINT NOT NULL UNIQUE,
    solicitud_id BIGINT NOT NULL,
    proveedor_id BIGINT NOT NULL,
    proveedor_razon_social VARCHAR(200) NOT NULL,
    cotizacion_id BIGINT,
    tipo VARCHAR(10) NOT NULL
      CHECK (tipo IN ('PRODUCTO', 'SERVICIO')),
    detalle TEXT,
    cantidad NUMERIC(12,2) NOT NULL,
    moneda CHAR(3) NOT NULL,
    precio_unitario NUMERIC(14,2) NOT NULL,
    precio_total NUMERIC(16,2) NOT NULL,
    monto_envio NUMERIC(14,2),
    estado VARCHAR(10) NOT NULL DEFAULT 'GENERADA'
      CHECK (estado IN ('GENERADA', 'ENVIADA')),
    emitida_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    enviada_por BIGINT,
    enviada_en TIMESTAMPTZ
);
