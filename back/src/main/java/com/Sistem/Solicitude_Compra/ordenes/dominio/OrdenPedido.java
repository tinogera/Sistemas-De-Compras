package com.Sistem.Solicitude_Compra.ordenes.dominio;

import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(schema = "ordenes", name = "orden_pedido")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrdenPedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long numero;

    @Column(name = "item_id", nullable = false, unique = true)
    private Long itemId;

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "proveedor_razon_social", nullable = false, length = 200)
    private String proveedorRazonSocial;

    @Column(name = "cotizacion_id")
    private Long cotizacionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoItem tipo;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private Moneda moneda;

    @Column(name = "precio_unitario",  nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "precio_total", nullable = false, precision = 16, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "monto_envio", precision = 14, scale = 2)
    private BigDecimal montoEnvio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length=10)
    private EstadoOrden estado = EstadoOrden.GENERADA;

    @Column(name = "emitida_en", nullable = false)
    private Instant emitidaEn;

    @Column(name = "enviada_por")
    private Long enviadaPor;

    @Column(name = "enviada_en")
    private Instant enviadaEn;

    public OrdenPedido(Long numero, Long itemId, Long solicitudId, Long proveedorId,
                       String proveedorRazonSocial, Long cotizacionId, TipoItem tipo,
                       String detalle, BigDecimal cantidad, Moneda moneda,
                       BigDecimal precioTotal) {
        this.numero = numero;
        this.itemId = itemId;
        this.solicitudId = solicitudId;
        this.proveedorId = proveedorId;
        this.proveedorRazonSocial = proveedorRazonSocial;
        this.cotizacionId = cotizacionId;
        this.tipo = tipo;
        this.detalle = detalle;
        this.cantidad = cantidad;
        this.moneda = moneda;
        this.precioTotal = precioTotal;
        this.precioUnitario = calcularPrecioUnitario(precioTotal, cantidad);
        this.emitidaEn = Instant.now();
    }

    public void actualizarMontos(BigDecimal precioTotal, BigDecimal montoEnvio) {
        if (precioTotal == null || precioTotal.signum() <= 0) {
            throw new ReglaNegocioException("PRECIO_INVALIDO", "El precio total tiene que ser mayor a cero");
        }
        if (montoEnvio != null && montoEnvio.signum() < 0) {
            throw new ReglaNegocioException("MONTO_ENVIO_INVALIDO", "El monto de envío no puede ser negativo");
        }
        this.precioTotal = precioTotal;
        this.precioUnitario = calcularPrecioUnitario(precioTotal, cantidad);
        this.montoEnvio = montoEnvio;
    }

    private static BigDecimal calcularPrecioUnitario(BigDecimal precioTotal, BigDecimal cantidad) {
        return precioTotal.divide(cantidad, 2, RoundingMode.HALF_UP);
    }
}

