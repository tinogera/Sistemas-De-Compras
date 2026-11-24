package com.Sistem.Solicitude_Compra.auditoria.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Marca que un evento ya se registró en el historial, para no registrarlo dos veces. */
@Entity
@Table(schema = "auditoria", name = "evento_procesado")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoProcesado {

    @Id
    @Column(name = "evento_id")
    private UUID eventoId;

    @Column(name = "procesado_en", nullable = false)
    private Instant procesadoEn;

    public EventoProcesado(UUID eventoId) {
        this.eventoId = eventoId;
        this.procesadoEn = Instant.now();
    }
}
