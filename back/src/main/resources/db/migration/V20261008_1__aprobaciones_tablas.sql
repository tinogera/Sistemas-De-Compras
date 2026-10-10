CREATE SCHEMA IF NOT EXISTS aprobaciones;

CREATE TABLE aprobaciones.regla (
  id              BIGSERIAL PRIMARY KEY,
  nombre          VARCHAR(120) NOT NULL UNIQUE,
  prioridad       INT NOT NULL,
  activa          BOOLEAN NOT NULL DEFAULT TRUE,
  por_defecto     BOOLEAN NOT NULL DEFAULT FALSE,
  creada_por      BIGINT,
  creada_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
  modificada_por  BIGINT,
  modificada_en   TIMESTAMPTZ,
  version         INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_regla_por_defecto ON aprobaciones.regla (por_defecto) WHERE por_defecto;

CREATE TABLE aprobaciones.regla_condicion (
  id              BIGSERIAL PRIMARY KEY,
  regla_id        BIGINT NOT NULL REFERENCES aprobaciones.regla(id),
  tipo            VARCHAR(20) NOT NULL,          -- por ahora solo monto
  operador        VARCHAR(15) NOT NULL CHECK (operador IN ('MAYOR','MAYOR_IGUAL','MENOR','MENOR_IGUAL')),
  valor_numerico  NUMERIC(16,2),
  moneda          CHAR(3) CHECK (moneda IN ('ARS','USD')),
  valor_id        BIGINT
);

CREATE TABLE aprobaciones.aprobacion_item (
                                              item_id                 BIGINT PRIMARY KEY,
                                              solicitud_id            BIGINT NOT NULL,
                                              solicitante_id          BIGINT NOT NULL,
                                              sector_id               BIGINT NOT NULL,
                                              categoria_id            BIGINT NOT NULL,
                                              tipo                    VARCHAR(10) NOT NULL,
                                              detalle                 TEXT,
                                              cantidad                NUMERIC(12,2) NOT NULL,
                                              urgencia                VARCHAR(5) NOT NULL,
                                              precio_estimado         NUMERIC(16,2) NOT NULL,
                                              moneda_estimada         CHAR(3) NOT NULL,
                                              precio_total            NUMERIC(16,2),
                                              moneda_total            CHAR(3),
                                              proveedor_razon_social  VARCHAR(200),
                                              regla_id                BIGINT,
                                              regla_nombre            VARCHAR(120),
                                              estado                  VARCHAR(10) NOT NULL DEFAULT 'EN_CURSO'
                                                  CHECK (estado IN ('EN_CURSO','APROBADA','RECHAZADA','CANCELADA')),
                                              cerrada                 BOOLEAN NOT NULL DEFAULT FALSE,
                                              solicitado_en           TIMESTAMPTZ NOT NULL,
                                              version                 INT NOT NULL DEFAULT 0
);

CREATE TABLE aprobaciones.regla_paso (
  id              BIGSERIAL PRIMARY KEY,
  regla_id        BIGINT NOT NULL REFERENCES aprobaciones.regla(id),
  orden           INT NOT NULL CHECK (orden >= 1),
  tipo_aprobador  VARCHAR(6) NOT NULL CHECK (tipo_aprobador IN ('ROL','SECTOR')),
  rol             VARCHAR(20),
  sector_id       BIGINT,
  CHECK ((tipo_aprobador = 'ROL' AND rol IS NOT NULL AND sector_id IS NULL)
      OR (tipo_aprobador = 'SECTOR' AND sector_id IS NOT NULL AND rol IS NULL))
);

CREATE TABLE aprobaciones.paso_item (
  id                BIGSERIAL PRIMARY KEY,
  item_id           BIGINT NOT NULL REFERENCES aprobaciones.aprobacion_item(item_id),
  orden             INT NOT NULL,
  tipo_aprobador    VARCHAR(6) NOT NULL CHECK (tipo_aprobador IN ('ROL','SECTOR')),
  rol               VARCHAR(20),
  sector_id         BIGINT,
  nombre_aprobador  VARCHAR(120) NOT NULL,
  estado            VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE'
                    CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO','OMITIDO','CANCELADO')),
  decidido_por      BIGINT,
  decidido_en       TIMESTAMPTZ,
  motivo            TEXT,
  version           INT NOT NULL DEFAULT 0,
  CHECK ((tipo_aprobador = 'ROL' AND rol IS NOT NULL AND sector_id IS NULL)
      OR (tipo_aprobador = 'SECTOR' AND sector_id IS NOT NULL AND rol IS NULL)),
  CHECK (estado <> 'RECHAZADO' OR motivo IS NOT NULL)
);
CREATE INDEX ix_paso_item ON aprobaciones.paso_item (item_id, orden);
CREATE INDEX ix_paso_pendiente ON aprobaciones.paso_item (tipo_aprobador, rol, sector_id) WHERE estado = 'PENDIENTE';

CREATE TABLE aprobaciones.evento_procesado (
  evento_id     UUID PRIMARY KEY,
  procesado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO aprobaciones.regla (nombre, prioridad, por_defecto) VALUES ('Por defecto', 1000, TRUE);
INSERT INTO aprobaciones.regla_paso (regla_id, orden, tipo_aprobador, rol)
  SELECT id, 1, 'ROL', 'SUPERVISOR' FROM aprobaciones.regla WHERE por_defecto;