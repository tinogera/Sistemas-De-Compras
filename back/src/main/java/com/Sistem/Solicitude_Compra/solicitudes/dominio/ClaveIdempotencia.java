package com.Sistem.Solicitude_Compra.solicitudes.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

/**
 * Clave que manda el front (header Idempotency-Key) para que reintentar un POST no cree otra solicitud.
 * El id lo asigna el cliente, así que implementa Persistable: sin eso Spring Data haría un merge
 * (SELECT + UPDATE) en vez de un INSERT, y la PRIMARY KEY nunca frenaría a la segunda inserción.
 */
@Entity
@Table(name = "clave_idempotencia", schema = "solicitudes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaveIdempotencia implements Persistable<UUID> {

    @Id
    private UUID clave;

    @Column(name = "solicitante_id", nullable = false)
    private Long solicitanteId;

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private OffsetDateTime creadaEn = OffsetDateTime.now(ZoneOffset.UTC);

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean nueva = true;

    public ClaveIdempotencia(UUID clave, Long solicitanteId, Long solicitudId) {
        this.clave = clave;
        this.solicitanteId = solicitanteId;
        this.solicitudId = solicitudId;
    }

    @Override
    public UUID getId() {
        return clave;
    }

    @Override
    public boolean isNew() {
        return nueva;
    }

    @PostLoad
    @PostPersist
    void marcarComoExistente() {
        nueva = false;
    }
}
