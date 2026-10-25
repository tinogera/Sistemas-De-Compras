CREATE SCHEMA IF NOT EXISTS compras;

CREATE TABLE compras.gestion_item (
      item_id                 BIGINT        PRIMARY KEY,
      solicitud_id            BIGINT        NOT NULL,
      sector_id               BIGINT        NOT NULL,
      categoria_id            BIGINT,
      tipo                    VARCHAR(10)   NOT NULL,
      detalle                 TEXT,
      cantidad                NUMERIC(12,2) NOT NULL,
      urgencia                VARCHAR(20)   NOT NULL,
      prioridad               SMALLINT      NOT NULL,
      fecha_necesaria         DATE,
      solicitado_en           TIMESTAMPTZ   NOT NULL,
      precio_estimado         NUMERIC(16,2) NOT NULL,
      moneda_estimada         CHAR(3)       NOT NULL,
      precio_total            NUMERIC(16,2),
      moneda_total            CHAR(3),
      monto_envio             NUMERIC(14,2),
      estado                  VARCHAR(20)   NOT NULL,
      estado_aprobacion       VARCHAR(10)   NOT NULL DEFAULT 'EN_CURSO',
      encargado_id            BIGINT,
      cotizacion_elegida_id   BIGINT,
      proveedor_compra_id     BIGINT,
      comprado_en             TIMESTAMPTZ,
      motivo_rechazo          TEXT,
      resuelto_en             TIMESTAMPTZ,
      ultimo_recordatorio_en  TIMESTAMPTZ,
      version                 BIGINT        NOT NULL DEFAULT 0,

      CONSTRAINT ck_gestion_item_comprado_completo
          CHECK (estado <> 'COMPRADO'
              OR (precio_total IS NOT NULL
                  AND proveedor_compra_id IS NOT NULL
                  AND estado_aprobacion = 'APROBADA')),

      CONSTRAINT ck_gestion_item_rechazado_con_motivo
          CHECK (estado <> 'RECHAZADO' OR motivo_rechazo IS NOT NULL)
);

CREATE INDEX idx_gestion_item_bandeja
    ON compras.gestion_item (estado, prioridad, fecha_necesaria);