CREATE INDEX idx_gestion_item_bandeja_activa
    ON compras.gestion_item (prioridad, fecha_necesaria, encargado_id)
    WHERE estado IN ('PENDIENTE', 'EN_COTIZACION');