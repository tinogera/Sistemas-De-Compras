package com.Sistem.Solicitude_Compra.reportes.dominio;

import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Una fila por ítem resuelto, para medir cuánto tardó desde que se creó hasta que se compró o rechazó.
 * El id es el del ítem (viene del evento), por eso Persistable: ver {@link HechoGasto}.
 */
@Entity
@Table(schema = "reportes", name = "hecho_resolucion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HechoResolucion implements Persistable<Long> {

    @Id
    @Column(name = "item_id")
    private Long itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Urgencia urgencia;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "resuelto_en", nullable = false)
    private Instant resueltoEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultadoResolucion resultado;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean nuevo = true;

    public HechoResolucion(Long itemId, Urgencia urgencia, Instant creadoEn, Instant resueltoEn,
                           ResultadoResolucion resultado) {
        if (itemId == null || urgencia == null || creadoEn == null || resueltoEn == null || resultado == null) {
            throw new ReglaNegocioException("HECHO_RESOLUCION_INCOMPLETO",
                    "Faltan datos para registrar la resolución");
        }
        if (resueltoEn.isBefore(creadoEn)) {
            throw new ReglaNegocioException("RESOLUCION_ANTES_DE_CREACION",
                    "El ítem no puede resolverse antes de haberse creado");
        }
        this.itemId = itemId;
        this.urgencia = urgencia;
        this.creadoEn = creadoEn;
        this.resueltoEn = resueltoEn;
        this.resultado = resultado;
    }

    @Override
    public Long getId() {
        return itemId;
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
