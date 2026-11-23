-- Historial de auditoría: solo se insertan filas, nunca se editan ni se borran.
CREATE TABLE auditoria.historial (
  id               BIGSERIAL PRIMARY KEY,
  entidad          VARCHAR(30) NOT NULL,
  entidad_id       BIGINT NOT NULL,
  accion           VARCHAR(30) NOT NULL,
  estado_anterior  VARCHAR(20),
  estado_nuevo     VARCHAR(20),
  usuario_id       BIGINT,
  comentario       TEXT,
  ocurrido_en      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_historial_entidad ON auditoria.historial (entidad, entidad_id, ocurrido_en);

-- Eventos ya procesados por los listeners de auditoría (idempotencia).
CREATE TABLE auditoria.evento_procesado (
  evento_id     UUID PRIMARY KEY,
  procesado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- A qué solicitud y solicitante pertenece cada ítem, para consultar el historial sin pedirle datos a Solicitudes.
CREATE TABLE auditoria.item_solicitud (
  item_id         BIGINT PRIMARY KEY,
  solicitud_id    BIGINT NOT NULL,
  solicitante_id  BIGINT NOT NULL
);
CREATE INDEX ix_item_solicitud_solicitud ON auditoria.item_solicitud (solicitud_id);
