package com.Sistem.Solicitude_Compra.compras.dominio;

import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(schema = "compras", name = "gestion_item")
public class GestionItem {

    // El id lo asigna el módulo de solicitudes (es el id del ítem): no se genera acá.
    @Id
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoItem tipo;

    @Column(columnDefinition = "text")
    private String detalle;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Urgencia urgencia;

    @Column(nullable = false)
    private Short prioridad;

    @Column(name = "fecha_necesaria")
    private LocalDate fechaNecesaria;

    @Column(name = "solicitado_en", nullable = false)
    private OffsetDateTime solicitadoEn;

    @Column(name = "precio_estimado", nullable = false, precision = 16, scale = 2)
    private BigDecimal precioEstimado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "moneda_estimada", nullable = false, length = 3)
    private Moneda monedaEstimada;

    @Column(name = "precio_total", precision = 16, scale = 2)
    private BigDecimal precioTotal;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "moneda_total", length = 3)
    private Moneda monedaTotal;

    @Column(name = "monto_envio", precision = 14, scale = 2)
    private BigDecimal montoEnvio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoGestion estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_aprobacion", nullable = false, length = 10)
    private EstadoAprobacion estadoAprobacion;

    @Column(name = "encargado_id")
    private Long encargadoId;

    @Column(name = "cotizacion_elegida_id")
    private Long cotizacionElegidaId;

    @Column(name = "proveedor_compra_id")
    private Long proveedorCompraId;

    @Column(name = "comprado_en")
    private OffsetDateTime compradoEn;

    @Column(name = "motivo_rechazo", columnDefinition = "text")
    private String motivoRechazo;

    @Column(name = "resuelto_en")
    private OffsetDateTime resueltoEn;

    @Column(name = "ultimo_recordatorio_en")
    private OffsetDateTime ultimoRecordatorioEn;

    // Sin valor inicial: con version null Spring Data trata la entidad como nueva (persist, no merge).
    @Version
    @Column(nullable = false)
    private Long version;

    public GestionItem(Long itemId, Long solicitudId, Long sectorId, Long categoriaId,
                       TipoItem tipo, String detalle, BigDecimal cantidad, Urgencia urgencia,
                       LocalDate fechaNecesaria, OffsetDateTime solicitadoEn,
                       BigDecimal precioEstimado, Moneda monedaEstimada) {
        this.itemId = itemId;
        this.solicitudId = solicitudId;
        this.sectorId = sectorId;
        this.categoriaId = categoriaId;
        this.tipo = tipo;
        this.detalle = detalle;
        this.cantidad = cantidad;
        this.urgencia = urgencia;
        this.prioridad = (short) urgencia.prioridad();
        this.fechaNecesaria = fechaNecesaria;
        this.solicitadoEn = solicitadoEn;
        this.precioEstimado = precioEstimado;
        this.monedaEstimada = monedaEstimada;
        this.estado = EstadoGestion.PENDIENTE;
        this.estadoAprobacion = EstadoAprobacion.EN_CURSO;
    }

    public boolean estaPendiente() {
        return estado == EstadoGestion.PENDIENTE;
    }

    /**
     * Copia los datos editados en Solicitudes, pero solo mientras el ítem siga PENDIENTE: si un
     * Encargado ya lo tomó, lo que cotiza no cambia por debajo. Devuelve si aplicó el cambio.
     */
    public boolean actualizarDatos(Long categoriaId, BigDecimal cantidad, String detalle,
                                   BigDecimal precioEstimado, Moneda monedaEstimada) {
        if (!estaPendiente()) {
            return false;
        }
        this.categoriaId = categoriaId;
        this.cantidad = cantidad;
        this.detalle = detalle;
        this.precioEstimado = precioEstimado;
        this.monedaEstimada = monedaEstimada;
        return true;
    }

    public void tomar(Long encargadoId) {
        if (estado != EstadoGestion.PENDIENTE) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "Solo se puede tomar un ítem PENDIENTE (estado actual: " + estado + ")");
        }
        this.encargadoId = encargadoId;
        this.estado = EstadoGestion.EN_COTIZACION;
    }

    public void reasignar(Long nuevoEncargadoId) {
        if (estaCerrada()) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "No se puede reasignar un ítem " + estado);
        }
        this.encargadoId = nuevoEncargadoId;
    }

    public void fijarPrecioTotal(BigDecimal importe, Moneda moneda, Long cotizacionId) {
        if (estado != EstadoGestion.EN_COTIZACION) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "Solo se puede fijar el precio total de un ítem EN_COTIZACION (estado actual: " + estado + ")");
        }
        this.precioTotal = importe;
        this.monedaTotal = moneda;
        this.cotizacionElegidaId = cotizacionId;
    }

    public void registrarCompra(Long proveedorId, OffsetDateTime fechaCompra) {
        if (estado != EstadoGestion.EN_COTIZACION) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "Solo se puede registrar la compra de un ítem EN_COTIZACION (estado actual: " + estado + ")");
        }
        if (estadoAprobacion != EstadoAprobacion.APROBADA) {
            throw new ReglaNegocioException("GESTION_SIN_APROBACION",
                    "No se puede registrar la compra: la cadena de aprobación está " + estadoAprobacion);
        }
        if (precioTotal == null) {
            throw new ReglaNegocioException("GESTION_SIN_PRECIO",
                    "No se puede registrar la compra sin precio total");
        }
        if (proveedorId == null) {
            throw new ReglaNegocioException("GESTION_SIN_PROVEEDOR",
                    "No se puede registrar la compra sin proveedor");
        }
        this.proveedorCompraId = proveedorId;
        this.compradoEn = fechaCompra;
        this.resueltoEn = fechaCompra;
        this.estado = EstadoGestion.COMPRADO;
    }

    public void corregirCompra(BigDecimal nuevoPrecio, Moneda nuevaMoneda, Long nuevoProveedorId) {
        if (estado != EstadoGestion.COMPRADO) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "Solo se puede corregir la compra de un ítem COMPRADO (estado actual: " + estado + ")");
        }
        if (nuevoPrecio == null || nuevoProveedorId == null) {
            throw new ReglaNegocioException("GESTION_COMPRA_INCOMPLETA",
                    "Una compra corregida debe tener precio total y proveedor");
        }
        this.precioTotal = nuevoPrecio;
        this.monedaTotal = nuevaMoneda;
        this.proveedorCompraId = nuevoProveedorId;
    }

    public void rechazar(String motivo, OffsetDateTime fechaResolucion) {
        if (estado == EstadoGestion.COMPRADO || estado == EstadoGestion.CANCELADO) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "No se puede rechazar un ítem " + estado);
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("GESTION_MOTIVO_REQUERIDO",
                    "El rechazo requiere un motivo");
        }
        this.motivoRechazo = motivo;
        this.resueltoEn = fechaResolucion;
        this.estado = EstadoGestion.RECHAZADO;
    }

    public void cancelar(OffsetDateTime fechaResolucion) {
        if (estado == EstadoGestion.COMPRADO || estado == EstadoGestion.RECHAZADO) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "No se puede cancelar un ítem " + estado);
        }
        this.resueltoEn = fechaResolucion;
        this.estado = EstadoGestion.CANCELADO;
    }

    // Estados terminales para reasignar: COMPRADO, RECHAZADO y CANCELADO.
    private boolean estaCerrada() {
        return estado == EstadoGestion.COMPRADO
                || estado == EstadoGestion.RECHAZADO
                || estado == EstadoGestion.CANCELADO;
    }
}