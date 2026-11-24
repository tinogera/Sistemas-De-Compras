package com.Sistem.Solicitude_Compra.aprobaciones.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Marca que un evento ya se aplicó a las cadenas, para los que no son idempotentes por sí solos (ItemEditado). */
@Entity
@Table(schema = "aprobaciones", name = "evento_procesado")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoAprobacionProcesado {

    @Id
    @Column(name = "evento_id")
    private UUID eventoId;

    @Column(name = "procesado_en", nullable = false)
    private Instant procesadoEn;

    public EventoAprobacionProcesado(UUID eventoId) {
        this.eventoId = eventoId;
        this.procesadoEn = Instant.now();
    }
}
