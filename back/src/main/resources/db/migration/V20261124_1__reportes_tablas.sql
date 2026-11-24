-- Modelo de lectura de Reportes (diseño §6.6). Solo lo llenan los listeners del módulo.

-- Una fila por orden generada. item_id es único para encontrar la fila cuando llega CompraActualizada.
CREATE TABLE reportes.hecho_gasto (
  orden_id      BIGINT PRIMARY KEY,
  item_id       BIGINT NOT NULL UNIQUE,
  fecha         DATE NOT NULL,
  sector_id     BIGINT NOT NULL,
  categoria_id  BIGINT NOT NULL,
  proveedor_id  BIGINT NOT NULL,
  moneda        CHAR(3) NOT NULL CHECK (moneda IN ('ARS', 'USD')),
  importe       NUMERIC(16,2) NOT NULL,
  monto_envio   NUMERIC(14,2) NOT NULL DEFAULT 0
);
CREATE INDEX ix_gasto_fecha ON reportes.hecho_gasto (fecha, moneda);

-- Una fila por ítem resuelto (comprado o rechazado). Incluye la espera de aprobaciones y compra.
CREATE TABLE reportes.hecho_resolucion (
  item_id      BIGINT PRIMARY KEY,
  urgencia     VARCHAR(5) NOT NULL CHECK (urgencia IN ('BAJA', 'MEDIA', 'ALTA')),
  creado_en    TIMESTAMPTZ NOT NULL,
  resuelto_en  TIMESTAMPTZ NOT NULL,
  resultado    VARCHAR(10) NOT NULL CHECK (resultado IN ('COMPRADO', 'RECHAZADO'))
);
CREATE INDEX ix_resolucion_resuelto_en ON reportes.hecho_resolucion (resuelto_en);

-- Eventos ya procesados por los listeners de Reportes (idempotencia, T12.2).
CREATE TABLE reportes.evento_procesado (
  evento_id     UUID PRIMARY KEY,
  procesado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);
