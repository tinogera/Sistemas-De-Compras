package com.Sistem.Solicitude_Compra.aprobaciones.dominio;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoCadena;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoPaso;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

@Entity
@Getter
@Setter
@Table(schema = "aprobaciones", name = "aprobacion_item")
public class AprobacionItem {

    // El id lo asigna el módulo de solicitudes (es el id del ítem): no se genera acá.
    @Id
    @Column(name = "item_id")
    private Long itemId;

    @Column(nullable = false)
    private Long solicitudId;

    @Column(nullable = false)
    private Long solicitanteId;

    @Column(nullable = false)
    private Long sectorId;

    @Column(nullable = false)
    private Long categoriaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoItem tipo;

    @Column(columnDefinition = "text")
    private String detalle;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Urgencia urgencia;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal precioEstimado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private Moneda monedaEstimada;

    @Column(precision = 16, scale = 2)
    private BigDecimal precioTotal;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 3)
    private Moneda monedaTotal;

    @Column(length = 200)
    private String proveedorRazonSocial;

    // Regla que se aplicó (copia del id y del nombre: la regla puede editarse después).
    private Long reglaId;

    @Column(length = 120)
    private String reglaNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoCadena estado = EstadoCadena.EN_CURSO;

    @Column(nullable = false)
    private boolean cerrada = false;

    @Column(nullable = false)
    private Instant solicitadoEn;

    // Sin valor inicial. Importante acá: el id es asignado, y con version != null
    // Spring Data haría merge en vez de persist y el insert fallaría.
    @Version
    @Setter(AccessLevel.NONE)
    private Integer version;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
    @Setter(AccessLevel.NONE)
    private List<PasoItem> pasos = new ArrayList<>();

    public void agregarPaso(PasoItem paso) {
        paso.setItem(this);
        pasos.add(paso);
    }

    /**
     * Pasos PENDIENTES cuyo orden es el menor entre los pendientes.
     * Si hay varios con el mismo orden se devuelven todos (van en paralelo).
     * Sin pasos pendientes devuelve una lista vacía.
     */
    public List<PasoItem> pasosActivos() {
        OptionalInt menor = pasos.stream()
                .filter(p -> p.getEstado() == EstadoPaso.PENDIENTE)
                .mapToInt(PasoItem::getOrden)
                .min();
        if (menor.isEmpty()) {
            return List.of();
        }
        int orden = menor.getAsInt();
        return pasos.stream()
                .filter(p -> p.getEstado() == EstadoPaso.PENDIENTE && p.getOrden() == orden)
                .toList();
    }
}