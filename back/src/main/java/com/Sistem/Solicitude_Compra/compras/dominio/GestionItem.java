package com.Sistem.Solicitude_Compra.compras.dominio;

import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
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

    /**
     * El Encargado toma el ítem: PENDIENTE pasa a EN_COTIZACION, o queda suyo uno EN_COTIZACION sin encargado
     * (cuando se quitó el rol al que lo tenía). Si ya es de otro, 409 YA_ASIGNADO. Devuelve si hubo cambio
     * (tomar dos veces un ítem propio no hace nada).
     */
    public boolean tomar(Long encargadoId) {
        if (estado == EstadoGestion.EN_COTIZACION) {
            if (this.encargadoId == null) {
                this.encargadoId = encargadoId;
                return true;
            }
            if (this.encargadoId.equals(encargadoId)) {
                return false;
            }
            throw new ConflictoException("YA_ASIGNADO", "Este ítem ya lo tomó otro Encargado.");
        }
        if (estado != EstadoGestion.PENDIENTE) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "Solo se puede tomar un ítem PENDIENTE (estado actual: " + estado + ")");
        }
        this.encargadoId = encargadoId;
        this.estado = EstadoGestion.EN_COTIZACION;
        return true;
    }

    /** Pasa a ser del Encargado que lo pide. Solo si está EN_COTIZACION y lo tiene otro Encargado. */
    public void reasignar(Long nuevoEncargadoId) {
        if (estado != EstadoGestion.EN_COTIZACION) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "Solo se puede reasignar un ítem EN_COTIZACION (estado actual: " + estado + ")");
        }
        if (encargadoId == null) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "El ítem no tiene Encargado: tomalo en vez de reasignarlo.");
        }
        if (encargadoId.equals(nuevoEncargadoId)) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO", "Este ítem ya es tuyo.");
        }
        this.encargadoId = nuevoEncargadoId;
    }

    /** El Encargado dejó de serlo: el ítem sigue EN_COTIZACION pero vuelve a la bandeja general. */
    public void quitarEncargado() {
        if (estado != EstadoGestion.EN_COTIZACION) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "Solo se puede quitar el Encargado de un ítem EN_COTIZACION (estado actual: " + estado + ")");
        }
        this.encargadoId = null;
    }

    /** Para operar sobre la cotización: EN_COTIZACION (422) y asignado al usuario (403). */
    public void exigirAsignadoA(Long usuarioId) {
        if (estado != EstadoGestion.EN_COTIZACION) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "El ítem tiene que estar EN_COTIZACION (estado actual: " + estado + ")");
        }
        if (encargadoId == null || !encargadoId.equals(usuarioId)) {
            throw new SinPermisoException("Este ítem no está asignado a vos.");
        }
    }

    /** Para editar la compra ya hecha: COMPRADO (422) y comprada por el usuario (403). */
    public void exigirCompraPropia(Long usuarioId) {
        if (estado != EstadoGestion.COMPRADO) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "Solo se puede editar la compra de un ítem COMPRADO (estado actual: " + estado + ")");
        }
        if (encargadoId == null || !encargadoId.equals(usuarioId)) {
            throw new SinPermisoException("Esta compra no es tuya.");
        }
    }

    /** El listener de AprobacionCompletada copia acá el resultado de la cadena. Devuelve si hubo cambio. */
    public boolean marcarAprobada() {
        if (estaCerrada() || estadoAprobacion == EstadoAprobacion.APROBADA) {
            return false;
        }
        this.estadoAprobacion = EstadoAprobacion.APROBADA;
        return true;
    }

    /** El listener de AprobacionReabierta: el precio cambió y la cadena vuelve a correr. */
    public boolean reabrirAprobacion() {
        if (estaCerrada() || estadoAprobacion == EstadoAprobacion.EN_CURSO) {
            return false;
        }
        this.estadoAprobacion = EstadoAprobacion.EN_CURSO;
        return true;
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

    /** El ítem vuelve a evaluarse con el precio estimado. */
    public void quitarPrecioTotal() {
        if (estado != EstadoGestion.EN_COTIZACION) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "Solo se puede quitar el precio total de un ítem EN_COTIZACION (estado actual: " + estado + ")");
        }
        this.precioTotal = null;
        this.monedaTotal = null;
        this.cotizacionElegidaId = null;
    }

    /** Lo que tiene que estar listo para comprar, con el código de error que ve el front. */
    public void exigirListaParaComprar() {
        if (estadoAprobacion != EstadoAprobacion.APROBADA) {
            throw new ReglaNegocioException("APROBACION_PENDIENTE",
                    "Este ítem todavía no tiene todas las aprobaciones");
        }
        if (precioTotal == null) {
            throw new ReglaNegocioException("FALTA_PRECIO_TOTAL",
                    "Fijá el precio total antes de comprar");
        }
    }

    public void registrarCompra(Long proveedorId, OffsetDateTime fechaCompra) {
        if (estado != EstadoGestion.EN_COTIZACION) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "Solo se puede registrar la compra de un ítem EN_COTIZACION (estado actual: " + estado + ")");
        }
        if (estadoAprobacion != EstadoAprobacion.APROBADA) {
            throw new ReglaNegocioException("APROBACION_PENDIENTE",
                    "No se puede registrar la compra: la cadena de aprobación está " + estadoAprobacion);
        }
        if (precioTotal == null) {
            throw new ReglaNegocioException("FALTA_PRECIO_TOTAL",
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

    /**
     * Cambios posteriores a la compra. {@code nuevoPrecio} y {@code nuevoEnvio} en null no se tocan; un envío
     * en cero deja el ítem sin envío. La cadena de aprobación no se re-evalúa: la compra ya está hecha.
     */
    public void actualizarCompra(BigDecimal nuevoPrecio, BigDecimal nuevoEnvio) {
        if (estado != EstadoGestion.COMPRADO) {
            throw new ReglaNegocioException("GESTION_ESTADO_INVALIDO",
                    "Solo se puede editar la compra de un ítem COMPRADO (estado actual: " + estado + ")");
        }
        if (nuevoPrecio != null) {
            this.precioTotal = nuevoPrecio;
        }
        if (nuevoEnvio != null) {
            this.montoEnvio = nuevoEnvio.signum() == 0 ? null : nuevoEnvio;
        }
    }

    public void rechazar(String motivo, OffsetDateTime fechaResolucion) {
        if (estado == EstadoGestion.COMPRADO || estado == EstadoGestion.CANCELADO
                || estado == EstadoGestion.RECHAZADO) {
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

    /** COMPRADO, RECHAZADO o CANCELADO: ya no cambia. Los listeners de otros módulos lo usan para ser idempotentes. */
    public boolean estaResuelta() {
        return estaCerrada();
    }

    // Estados terminales para reasignar: COMPRADO, RECHAZADO y CANCELADO.
    private boolean estaCerrada() {
        return estado == EstadoGestion.COMPRADO
                || estado == EstadoGestion.RECHAZADO
                || estado == EstadoGestion.CANCELADO;
    }
}