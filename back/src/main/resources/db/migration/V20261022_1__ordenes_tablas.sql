CREATE TABLE catalogo.categoria (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    tipo VARCHAR(10) NOT NULL
        CHECK (tipo IN ('PRODUCTO', 'SERVICIO')),
    estado VARCHAR(10) NOT NULL DEFAULT 'NUEVA'
        CHECK (estado IN ('NUEVA', 'CONOCIDA', 'UNIFICADA')),
    unificada_en BIGINT REFERENCES catalogo.categoria(id)
);

CREATE UNIQUE INDEX ux_categoria_nombre
    ON catalogo.categoria (tipo, lower(nombre));

CREATE TABLE catalogo.proveedor (
    id BIGSERIAL PRIMARY KEY,
    razon_social VARCHAR(200) NOT NULL,
    cuit VARCHAR(13) UNIQUE,
    telefono VARCHAR(50),
    email VARCHAR(255),
    contacto VARCHAR(150),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE catalogo.proveedor_categoria (
    proveedor_id BIGINT NOT NULL
        REFERENCES catalogo.proveedor(id),
    categoria_id BIGINT NOT NULL
        REFERENCES catalogo.categoria(id),
    PRIMARY KEY (proveedor_id, categoria_id)
);

CREATE INDEX ix_provcat_categoria
    ON catalogo.proveedor_categoria (categoria_id);