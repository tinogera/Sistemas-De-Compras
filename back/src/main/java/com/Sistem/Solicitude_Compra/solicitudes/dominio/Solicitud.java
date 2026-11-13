package com.Sistem.Solicitude_Compra.solicitudes.dominio;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


import java.util.ArrayList;

@Entity
@Setter 
@Getter 
@Table(name = "solicitud", schema = "solicitudes")
public class Solicitud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "El solicitante es obligatorio")
    private Long solicitanteId;
    @NotNull(message = "El sector es obligatorio")
    private Long sectorId;

    @NotNull(message = "La urgencia es obligatoria")
    @Enumerated(EnumType.STRING)
    private Urgencia urgencia;
    @NotNull(message = "La fecha necesaria es obligatoria")
    private LocalDate fechaNecesaria;
    private String observaciones;
    
    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado = EstadoSolicitud.EN_REVISION;
    
    private LocalDateTime creadaEn = LocalDateTime.now();
    
    @Version
    private Long version;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotEmpty(message = "La solicitud necesita al menos un ítem")
    @Valid
    private List<Item> items = new ArrayList<>();

    public void agregarItem(Item item) {
        item.setSolicitud(this);
        this.items.add(item);
    }

    /**
     * Deriva el estado de la solicitud a partir del estado de sus ítems (ver T08.6):
     * todos CANCELADOS -> CANCELADA; todos en estado final (con al menos uno no cancelado) -> LISTA,
     * y CERRADA si además cada ítem COMPRADO ya tiene su orden enviada (si no hay ninguno comprado,
     * CERRADA enseguida); en cualquier otro caso, EN_REVISION. Un ítem aprobado pero sin comprar
     * no es final.
     */
    public void recalcularEstado() {
        if (items.isEmpty()) {
            return;
        }
        if (items.stream().allMatch(i -> i.getEstado() == EstadoItem.CANCELADO)) {
            estado = EstadoSolicitud.CANCELADA;
        } else if (!items.stream().allMatch(i -> i.getEstado().esFinal())) {
            estado = EstadoSolicitud.EN_REVISION;
        } else if (items.stream().filter(i -> i.getEstado() == EstadoItem.COMPRADO)
                .allMatch(i -> Boolean.TRUE.equals(i.getOrdenEnviada()))) {
            estado = EstadoSolicitud.CERRADA;
        } else {
            estado = EstadoSolicitud.LISTA;
        }
    }

    public boolean puedeCancelarse() {
        return items.stream().allMatch(item -> item.getEstado() == EstadoItem.PENDIENTE);
    }
}
