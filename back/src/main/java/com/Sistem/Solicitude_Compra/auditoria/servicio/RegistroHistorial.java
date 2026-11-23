package com.Sistem.Solicitude_Compra.auditoria.servicio;

import static com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial.APROBACION;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial.ASIGNACION;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial.CAMBIO_ESTADO;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial.CAMBIO_ROL;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial.CREACION;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial.EDICION;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial.RECHAZO;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial.REASIGNACION;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial.UNIFICACION;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial.CATEGORIA;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial.COTIZACION;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial.ITEM;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial.ORDEN;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial.REGLA;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial.SOLICITUD;
import static com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial.USUARIO;

import com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EntradaHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EventoProcesado;
import com.Sistem.Solicitude_Compra.auditoria.dominio.ItemSolicitud;
import com.Sistem.Solicitude_Compra.auditoria.repositorio.EventoProcesadoRepository;
import com.Sistem.Solicitude_Compra.auditoria.repositorio.HistorialRepository;
import com.Sistem.Solicitude_Compra.auditoria.repositorio.ItemSolicitudRepository;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionCompletada;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionReabierta;
import com.Sistem.Solicitude_Compra.compartido.eventos.CategoriaCreada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CategoriaUnificada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraRegistrada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CotizacionCargada;
import com.Sistem.Solicitude_Compra.compartido.eventos.EncargadoDesasignado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemAsignado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemCancelado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemEditado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenEnviada;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenGenerada;
import com.Sistem.Solicitude_Compra.compartido.eventos.PasoDecidido;
import com.Sistem.Solicitude_Compra.compartido.eventos.PrecioTotalActualizado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ReglaModificada;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCancelada;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCerrada;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudLista;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Traduce cada evento de cambio a una fila del historial (regla 11: todo cambio queda registrado).
 * Idempotente: si el evento ya se registró, no hace nada; el aviso y la fila se guardan en la misma transacción.
 */
@Service
public class RegistroHistorial {

    private final HistorialRepository historial;
    private final EventoProcesadoRepository procesados;
    private final ItemSolicitudRepository itemsSolicitud;
    private final AuthApi authApi;

    public RegistroHistorial(HistorialRepository historial, EventoProcesadoRepository procesados,
                             ItemSolicitudRepository itemsSolicitud, AuthApi authApi) {
        this.historial = historial;
        this.procesados = procesados;
        this.itemsSolicitud = itemsSolicitud;
        this.authApi = authApi;
    }

    @ApplicationModuleListener
    void alCrearseLaSolicitud(SolicitudCreada e) {
        registrar(e.eventoId(), () -> {
            guardar(SOLICITUD, e.solicitudId(), CREACION, null, "EN_REVISION", e.solicitanteId(), null,
                    e.ocurridoEn());
            e.items().forEach(item -> itemsSolicitud.save(
                    new ItemSolicitud(item.itemId(), e.solicitudId(), e.solicitanteId())));
        });
    }

    @ApplicationModuleListener
    void alEditarseUnItem(ItemEditado e) {
        registrar(e.eventoId(), () -> guardar(ITEM, e.itemId(), EDICION, null, null, e.usuarioId(),
                "Detalle: " + e.detalle() + " · Cantidad: " + e.cantidad() + " · Precio estimado: "
                        + e.precioEstimado() + " " + e.monedaEstimada(), e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alCancelarseUnItem(ItemCancelado e) {
        registrar(e.eventoId(), () -> guardar(ITEM, e.itemId(), CAMBIO_ESTADO, "PENDIENTE", "CANCELADO",
                e.usuarioId(), null, e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alCancelarseLaSolicitud(SolicitudCancelada e) {
        registrar(e.eventoId(), () -> guardar(SOLICITUD, e.solicitudId(), CAMBIO_ESTADO, "EN_REVISION",
                "CANCELADA", e.usuarioId(), null, e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alAsignarseUnItem(ItemAsignado e) {
        registrar(e.eventoId(), () -> {
            boolean habiaEncargado = e.encargadoAnteriorId() != null;
            guardar(ITEM, e.itemId(), habiaEncargado ? REASIGNACION : ASIGNACION,
                    habiaEncargado ? "EN_COTIZACION" : "PENDIENTE", "EN_COTIZACION", e.encargadoNuevoId(),
                    e.encargadoNuevoId() == null ? "Quedó sin encargado" : null, e.ocurridoEn());
        });
    }

    @ApplicationModuleListener
    void alCargarseUnaCotizacion(CotizacionCargada e) {
        registrar(e.eventoId(), () -> guardar(COTIZACION, e.cotizacionId(), CREACION, null, "CANDIDATA",
                e.usuarioId(), "Ítem " + e.itemId() + " · " + e.precioTotal() + " " + e.moneda(), e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alActualizarseElPrecioTotal(PrecioTotalActualizado e) {
        registrar(e.eventoId(), () -> {
            String comentario = "Precio total: " + e.precioTotal() + " " + e.moneda();
            if (e.proveedorRazonSocial() != null) {
                comentario += " · Proveedor: " + e.proveedorRazonSocial();
            }
            guardar(ITEM, e.itemId(), EDICION, null, null, e.usuarioId(), comentario, e.ocurridoEn());
        });
    }

    @ApplicationModuleListener
    void alDecidirseUnPaso(PasoDecidido e) {
        registrar(e.eventoId(), () -> {
            boolean rechazo = "RECHAZADO".equals(e.decision());
            String comentario = nombreDe(e.usuarioId());
            if (rechazo && e.motivo() != null) {
                comentario += " · Motivo: " + e.motivo();
            }
            guardar(ITEM, e.itemId(), rechazo ? RECHAZO : APROBACION, null, null, e.usuarioId(), comentario,
                    e.ocurridoEn());
        });
    }

    @ApplicationModuleListener
    void alCompletarseLaAprobacion(AprobacionCompletada e) {
        registrar(e.eventoId(), () -> guardar(ITEM, e.itemId(), CAMBIO_ESTADO, "EN_CURSO", "APROBADA", null,
                e.automatica() ? "automática" : null, e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alReabrirseLaAprobacion(AprobacionReabierta e) {
        registrar(e.eventoId(), () -> guardar(ITEM, e.itemId(), CAMBIO_ESTADO, "APROBADA", "EN_CURSO", null,
                e.motivo(), e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alRegistrarseLaCompra(CompraRegistrada e) {
        registrar(e.eventoId(), () -> guardar(ITEM, e.itemId(), CAMBIO_ESTADO, "EN_COTIZACION", "COMPRADO",
                e.encargadoId(), null, e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alActualizarseLaCompra(CompraActualizada e) {
        registrar(e.eventoId(), () -> guardar(ITEM, e.itemId(), EDICION, null, null, e.usuarioId(),
                "Precio total: " + e.precioTotalAnterior() + " → " + e.precioTotal() + " " + e.moneda()
                        + " · Envío: " + e.montoEnvioAnterior() + " → " + e.montoEnvio(), e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alModificarseUnaRegla(ReglaModificada e) {
        registrar(e.eventoId(), () -> {
            AccionHistorial accion = switch (String.valueOf(e.accion())) {
                case "CREADA" -> CREACION;
                case "EDITADA" -> EDICION;
                default -> CAMBIO_ESTADO;
            };
            guardar(REGLA, e.reglaId(), accion, null, null, e.usuarioId(), e.accion() + " · " + e.nombre(),
                    e.ocurridoEn());
        });
    }

    @ApplicationModuleListener
    void alResolverseUnItem(ItemResuelto e) {
        // La compra ya la registra CompraRegistrada: acá solo el rechazo.
        if (!"RECHAZADO".equals(e.resultado())) {
            return;
        }
        registrar(e.eventoId(), () -> guardar(ITEM, e.itemId(), CAMBIO_ESTADO, "EN_COTIZACION", "RECHAZADO",
                e.encargadoId(), "Rechazado por " + e.rechazadoPor() + " · Motivo: " + e.motivoRechazo(),
                e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alQuedarListaLaSolicitud(SolicitudLista e) {
        registrar(e.eventoId(), () -> guardar(SOLICITUD, e.solicitudId(), CAMBIO_ESTADO, null, "LISTA", null,
                null, e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alCerrarseLaSolicitud(SolicitudCerrada e) {
        registrar(e.eventoId(), () -> guardar(SOLICITUD, e.solicitudId(), CAMBIO_ESTADO, null, "CERRADA", null,
                null, e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alGenerarseLaOrden(OrdenGenerada e) {
        registrar(e.eventoId(), () -> guardar(ORDEN, e.ordenId(), CREACION, null, "GENERADA", null,
                "Orden N° " + e.numero(), e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alEnviarseLaOrden(OrdenEnviada e) {
        registrar(e.eventoId(), () -> guardar(ORDEN, e.ordenId(), CAMBIO_ESTADO, "GENERADA", "ENVIADA",
                e.usuarioId(), null, e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alCrearseUnaCategoria(CategoriaCreada e) {
        registrar(e.eventoId(), () -> guardar(CATEGORIA, e.categoriaId(), CREACION, null, null, null,
                e.nombre(), e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alUnificarseUnaCategoria(CategoriaUnificada e) {
        registrar(e.eventoId(), () -> guardar(CATEGORIA, e.origenId(), UNIFICACION, null, null, e.usuarioId(),
                "Unificada en la categoría " + e.destinoId(), e.ocurridoEn()));
    }

    @ApplicationModuleListener
    void alDesasignarseUnEncargado(EncargadoDesasignado e) {
        registrar(e.eventoId(), () -> guardar(USUARIO, e.usuarioId(), CAMBIO_ROL, "ENCARGADO", null, e.adminId(),
                "Dejó de ser Encargado", e.ocurridoEn()));
    }

    /** Ejecuta el registro una sola vez por evento. */
    private void registrar(UUID eventoId, Runnable registro) {
        if (procesados.existsById(eventoId)) {
            return;
        }
        registro.run();
        procesados.save(new EventoProcesado(eventoId));
    }

    private void guardar(EntidadHistorial entidad, Long entidadId, AccionHistorial accion, String anterior,
                         String nuevo, Long usuarioId, String comentario, Instant ocurridoEn) {
        historial.save(EntradaHistorial.de(entidad, entidadId, accion, anterior, nuevo, usuarioId, comentario,
                ocurridoEn));
    }

    private String nombreDe(Long usuarioId) {
        try {
            return authApi.obtenerUsuario(usuarioId).nombre();
        } catch (NoEncontradoException ex) {
            return "Usuario " + usuarioId;
        }
    }
}
