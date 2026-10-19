CREATE SCHEMA IF NOT EXISTS solicitudes;

CREATE TABLE solicitudes.solicitud (
    id BIGSERIAL PRIMARY KEY,
    solicitante_id VARCHAR(100) NOT NULL,
    sector_id VARCHAR(100) NOT NULL,
    urgencia VARCHAR(50) NOT NULL,
    fecha_necesaria DATE NOT NULL,
    observaciones TEXT,
    estado VARCHAR(50) NOT NULL DEFAULT 'EN_REVISION',
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE solicitudes.item (
    id BIGSERIAL PRIMARY KEY,
    solicitud_id BIGINT NOT NULL REFERENCES solicitudes.solicitud(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL,
    categoria_id VARCHAR(100) NOT NULL,
    detalle TEXT NOT NULL,
    cantidad NUMERIC(12, 2) NOT NULL CHECK (cantidad > 0),
    precio_estimado NUMERIC(16, 2) NOT NULL CHECK (precio_estimado > 0),
    moneda_estimada CHAR(3) NOT NULL CHECK (moneda_estimada IN ('ARS','USD')),
    precio_total NUMERIC(16, 2),
    moneda_total CHAR(3),
    estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'EN_COTIZACION', 'COMPRADO', 'RECHAZADO', 'CANCELADO')),
    estado_aprobacion VARCHAR(10) NOT NULL DEFAULT 'EN_CURSO' CHECK (estado_aprobacion IN ('EN_CURSO','APROBADA')),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    proveedor_comprado VARCHAR(200),
    motivo_rechazo TEXT,
    rechazado_por VARCHAR(200),
    orden_enviada BOOLEAN NOT NULL DEFAULT FALSE
);