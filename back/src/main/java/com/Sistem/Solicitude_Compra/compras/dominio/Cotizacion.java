package com.Sistem.Solicitude_Compra.compras.dominio;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "Cotizacion" , schema ="compras")
public class Cotizacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "proveedor_razon_social", length = 150)
    private String proveedorRazonSocial;

    @Column(name = "moneda", length = 10, nullable = false)
    private String moneda;

    @Column(name = "precio_unitario", precision = 15, scale = 4, nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "precio_total", precision = 15, scale = 4, nullable = false)
    private BigDecimal precioTotal;

    @Column(name = "fecha_validez")
    private LocalDate fechaValidez;

    @Column(name = "entrega_estimada")
    private LocalDate entregaEstimada;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "estado", length = 50, nullable = false)
    private String estado;

    @Column(name = "cargada_por", length = 100, nullable = false)
    private String cargadaPor;

    @Column(name = "cargada_en", nullable = false)
    private LocalDateTime cargadaEn;

    public boolean estaVigente(LocalDate hoy){
        return hoy.isAfter(LocalDate.now());
    }
}
