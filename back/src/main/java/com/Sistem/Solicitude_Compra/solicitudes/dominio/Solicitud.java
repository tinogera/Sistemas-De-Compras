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

    public boolean puedeCancelarse() {
        return items.stream().allMatch(item -> item.getEstado() == EstadoItem.PENDIENTE);
    }
}
