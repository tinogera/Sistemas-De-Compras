package com.Sistem.Solicitude_Compra.solicitudes.dominio;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;

import java.util.ArrayList;

@Entity
@Setter 
@Getter 
@Table(name = "item", schema = "solicitudes")
public class Item {
@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación ManyToOne con la Solicitud de Compra
    @NotNull(message = "La solicitud no puede ser nula")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoItem tipo;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    // Cantidad: 12 dígitos en total, 2 decimales, mayor estricto a 0
    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "La cantidad debe tener como máximo 10 dígitos enteros y 2 decimales")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal cantidad;

    @NotNull(message = "El precio estimado es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "El precio estimado debe ser mayor a 0")
    @Column(name = "precio_estimado", precision = 12, scale = 2, nullable = false)
    private BigDecimal precioEstimado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "moneda_estimada", nullable = false)
    private Moneda monedaEstimada;

    // Opcionales (pueden ser nulos)
    @Column(name = "precio_total", precision = 12, scale = 2)
    private BigDecimal precioTotal;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "moneda_total")
    private Moneda monedaTotal;

    // Estado por defecto: PENDIENTE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoItem estado = EstadoItem.PENDIENTE;

    // Estado de aprobación por defecto: EN_CURSO
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_aprobacion", nullable = false)
    private EstadoAprobacion estadoAprobacion = EstadoAprobacion.EN_CURSO;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @Version
    private Long version;

    @Column(name = "proveedor_comprado")
    private String proveedorComprado;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    @Column(name = "rechazado_por")
    private String rechazadoPor;

    @Column(name = "orden_enviada")
    private Boolean ordenEnviada = false;
}