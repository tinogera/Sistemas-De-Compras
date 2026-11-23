package com.Sistem.Solicitude_Compra.auditoria.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A qué solicitud y solicitante pertenece cada ítem; se llena al crearse la solicitud. */
@Entity
@Table(schema = "auditoria", name = "item_solicitud")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemSolicitud {

    @Id
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Column(name = "solicitante_id", nullable = false)
    private Long solicitanteId;

    public ItemSolicitud(Long itemId, Long solicitudId, Long solicitanteId) {
        this.itemId = itemId;
        this.solicitudId = solicitudId;
        this.solicitanteId = solicitanteId;
    }
}
