package com.Sistem.Solicitude_Compra.aprobaciones.dominio;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoPaso;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(schema = "aprobaciones", name = "paso_item")
public class PasoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private AprobacionItem item;

    @Column(nullable = false)
    private int orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private TipoAprobador tipoAprobador;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Rol rol;

    private Long sectorId;

    // Texto que se muestra en pantalla ("Supervisor", "Gerencia General").
    @Column(nullable = false, length = 120)
    private String nombreAprobador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoPaso estado = EstadoPaso.PENDIENTE;

    private Long decididoPor;

    private Instant decididoEn;

    // Obligatorio cuando el estado es RECHAZADO (CHECK en la base).
    @Column(columnDefinition = "text")
    private String motivo;

    @Version
    @Setter(AccessLevel.NONE)
    private Integer version;

    /**
     * true si el paso es de tipo ROL y el rol coincide, o es de tipo SECTOR y el sector coincide.
     * Solo compara al aprobador: que el paso esté pendiente lo garantiza AprobacionItem.pasosActivos().
     */
    public boolean puedeDecidir(Rol rol, Long sectorId) {
        return switch (tipoAprobador) {
            case ROL -> this.rol != null && this.rol == rol;
            case SECTOR -> this.sectorId != null && this.sectorId.equals(sectorId);
        };
    }
}
