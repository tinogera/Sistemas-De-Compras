package com.Sistem.Solicitude_Compra.compras.dominio;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(schema = "compras", name = "cotizacion")
public class Cotizacion {

    /** "Hoy" para vencimientos se calcula en hora de Argentina (caso borde 19). */
    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    // Copia de la razón social: evita consultar el catálogo cada vez que se listan cotizaciones.
    @Column(name = "proveedor_razon_social", length = 200)
    private String proveedorRazonSocial;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private Moneda moneda;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "precio_total", nullable = false, precision = 16, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "fecha_validez", nullable = false)
    private LocalDate fechaValidez;

    @Column(name = "entrega_estimada")
    private LocalDate entregaEstimada;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoCotizacion estado = EstadoCotizacion.CANDIDATA;

    @Column(name = "cargada_por", nullable = false)
    private Long cargadaPor;

    @Column(name = "cargada_en", nullable = false)
    private OffsetDateTime cargadaEn;

    public Cotizacion(Long itemId, Long proveedorId, String proveedorRazonSocial, Moneda moneda,
                      BigDecimal precioUnitario, BigDecimal precioTotal, LocalDate fechaValidez,
                      LocalDate entregaEstimada, String observaciones, Long cargadaPor,
                      OffsetDateTime cargadaEn) {
        this.itemId = itemId;
        this.proveedorId = proveedorId;
        this.proveedorRazonSocial = proveedorRazonSocial;
        this.moneda = moneda;
        this.precioUnitario = precioUnitario;
        this.precioTotal = precioTotal;
        this.fechaValidez = fechaValidez;
        this.entregaEstimada = entregaEstimada;
        this.observaciones = observaciones;
        this.cargadaPor = cargadaPor;
        this.cargadaEn = cargadaEn;
    }

    /** Vigente si {@code fechaValidez} es mayor o igual a hoy. "Vencida" no es un estado: se calcula. */
    public boolean estaVigente(LocalDate hoy) {
        return !fechaValidez.isBefore(hoy);
    }

    public boolean estaVigente() {
        return estaVigente(LocalDate.now(ZONA));
    }
}
