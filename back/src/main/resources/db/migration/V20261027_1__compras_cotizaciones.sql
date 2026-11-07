CREATE TABLE compras.cotizacion (
    id                      BIGSERIAL PRIMARY KEY,
    item_id                 BIGINT NOT NULL,
    proveedor_id            BIGINT NOT NULL,
    proveedor_razon_social  VARCHAR(150),
    moneda                  VARCHAR(10) NOT NULL,
    precio_unitario         NUMERIC(15, 4) NOT NULL,
    precio_total            NUMERIC(15, 4) NOT NULL,
    fecha_validez           DATE,
    entrega_estimada        DATE,
    observaciones           TEXT,
    estado                  VARCHAR(50) NOT NULL,
    cargada_por             VARCHAR(100) NOT NULL,
    cargada_en              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE UNIQUE INDEX ux_una_seleccionada 
ON compras.cotizacion (item_id) 
WHERE estado = 'SELECCIONADA';