-- La migración de gestion_item no validaba los estados: un typo dejaba un ítem fuera de todos los filtros de la bandeja.
ALTER TABLE compras.gestion_item
    ADD CONSTRAINT ck_gestion_item_estado
        CHECK (estado IN ('PENDIENTE', 'EN_COTIZACION', 'COMPRADO', 'RECHAZADO', 'CANCELADO')),
    ADD CONSTRAINT ck_gestion_item_estado_aprobacion
        CHECK (estado_aprobacion IN ('EN_CURSO', 'APROBADA')),
    ADD CONSTRAINT ck_gestion_item_tipo
        CHECK (tipo IN ('PRODUCTO', 'SERVICIO'));
