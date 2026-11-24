package com.Sistem.Solicitude_Compra.reportes.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/** Marca que un evento ya alimentó los reportes, para no contarlo dos veces (T12.2). */
@Entity(name = "EventoProcesadoReportes")
@Table(schema = "reportes", name = "evento_procesado")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoProcesadoReportes implements Persistable<UUID> {

    @Id
    @Column(name = "evento_id")
    private UUID eventoId;

    @Column(name = "procesado_en", nullable = false)
    private Instant procesadoEn;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean nuevo = true;

    public EventoProcesadoReportes(UUID eventoId) {
        this.eventoId = eventoId;
        this.procesadoEn = Instant.now();
    }

    @Override
    public UUID getId() {
        return eventoId;
    }

    @Override
    public boolean isNew() {
        return nuevo;
    }

    @PostLoad
    @PostPersist
    void marcarComoExistente() {
        nuevo = false;
    }
}
