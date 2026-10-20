// package com.Sistem.Solicitude_Compra.solicitudes.dominio;

// import jakarta.persistence.*;
// import jakarta.validation.constraints.NotNull;
// import lombok.Getter;
// import lombok.Setter;
// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.ArrayList;

// @Entity
// @Setter 
// @Getter 
// @Table(name = "solicitud", schema = "solicitudes")
// public class Solicitud {
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;
    
//     private String solicitanteId;
//     private String sectorId;
//     private String urgencia;
//     private LocalDate fechaNecesaria;
//     private String observaciones;
    
//     @Enumerated(EnumType.STRING)
//     private EstadoSolicitud estado = EstadoSolicitud.EN_REVISION;
    
//     private LocalDateTime creadaEn = LocalDateTime.now();
    
//     @Version
//     private Long version;

//     @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
//     private List<Item> items = new ArrayList<>();

//     public void agregarItem(Item item) {
//         item.setSolicitud(this);
//         this.items.add(item);
//     }

//     public boolean puedeCancelarse() {
//         return items.stream().allMatch(item -> item.getEstado() == EstadoItem.PENDIENTE);
//     }
//     // Validaciones de negocio adicionales según requerimientos...
// }