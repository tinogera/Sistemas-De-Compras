CREATE TABLE auth.sector (
    id      BIGSERIAL PRIMARY KEY,
    nombre  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE auth.usuario (
    id               BIGSERIAL PRIMARY KEY,
    email            VARCHAR(255) UNIQUE NOT NULL,
    nombre           VARCHAR(150) NOT NULL,
    sector_id BIGINT NOT NULL REFERENCES auth.sector(id),
    rol              VARCHAR(50) NOT NULL CHECK (rol IN ( 'SOLICITANTE', 'ENCARGADO', 'ADMIN', 'SUPERVISOR')),
    password_hash    VARCHAR(100) NOT NULL,
    activo BOOLEAN   NOT NULL DEFAULT TRUE
);