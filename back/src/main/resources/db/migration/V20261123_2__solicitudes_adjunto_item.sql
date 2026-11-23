CREATE TABLE solicitudes.adjunto_item (
  id            BIGSERIAL PRIMARY KEY,
  item_id       BIGINT NOT NULL REFERENCES solicitudes.item(id) ON DELETE CASCADE,
  clave_objeto  VARCHAR(500) NOT NULL,
  nombre        VARCHAR(255) NOT NULL,
  mime_type     VARCHAR(50) NOT NULL
                CHECK (mime_type IN ('application/pdf','image/jpeg','image/png')),
  tamano_bytes  BIGINT NOT NULL CHECK (tamano_bytes <= 10485760),
  subido_por    BIGINT NOT NULL,
  subido_en     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_adjunto_item_item ON solicitudes.adjunto_item (item_id, subido_en);
