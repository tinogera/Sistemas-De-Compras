-- T10.1: alinea compras.cotizacion con el diseño (§6.4). La V20261027_1 ya está mergeada y no se edita.
-- La tabla todavía no tiene filas (nada escribe en ella), así que los cambios de tipo son seguros.

ALTER TABLE compras.cotizacion
    ALTER COLUMN proveedor_razon_social TYPE VARCHAR(200),
    ALTER COLUMN moneda                 TYPE CHAR(3),
    ALTER COLUMN precio_unitario        TYPE NUMERIC(14, 2),
    ALTER COLUMN precio_total           TYPE NUMERIC(16, 2),
    ALTER COLUMN fecha_validez          SET NOT NULL,
    ALTER COLUMN estado                 TYPE VARCHAR(12),
    ALTER COLUMN estado                 SET DEFAULT 'CANDIDATA',
    ALTER COLUMN cargada_por            TYPE BIGINT USING cargada_por::BIGINT,
    ALTER COLUMN cargada_en             TYPE TIMESTAMPTZ USING cargada_en AT TIME ZONE 'UTC';

ALTER TABLE compras.cotizacion
    ADD CONSTRAINT fk_cotizacion_item FOREIGN KEY (item_id) REFERENCES compras.gestion_item (item_id),
    ADD CONSTRAINT ck_cotizacion_moneda CHECK (moneda IN ('ARS', 'USD')),
    ADD CONSTRAINT ck_cotizacion_precio_unitario CHECK (precio_unitario > 0),
    ADD CONSTRAINT ck_cotizacion_estado CHECK (estado IN ('CANDIDATA', 'SELECCIONADA', 'DESCARTADA'));

CREATE INDEX ix_cotizacion_item ON compras.cotizacion (item_id, cargada_en DESC);
