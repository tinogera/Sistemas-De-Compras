package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.AprobacionItem;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoCadena;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoPaso;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.EventoAprobacionProcesado;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.PasoItem;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.AprobacionItemRepository;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.EventoAprobacionProcesadoRepository;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.PasoItemRepository;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionCompletada;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionRechazada;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionReabierta;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemEditado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.eventos.PasoAprobacionActivado;
import com.Sistem.Solicitude_Compra.compartido.eventos.PasoDecidido;
import com.Sistem.Solicitude_Compra.compartido.eventos.PrecioTotalActualizado;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La cadena de aprobación de cada ítem: se crea con la solicitud, se re-evalúa cuando cambia el precio o el
 * ítem, y avanza cuando un aprobador decide un paso. Un paso se publica como activo una sola vez (así no se
 * mandan mails repetidos): al crearse la cadena, al completarse el orden anterior o al aparecer al re-evaluar.
 */
@Service
public class CadenaAprobacionService {

    private final AprobacionItemRepository aprobaciones;
    private final PasoItemRepository pasos;
    private final EventoAprobacionProcesadoRepository procesados;
    private final MotorDeReglas motor;
    private final AuthApi auth;
    private final ApplicationEventPublisher eventos;

    public CadenaAprobacionService(AprobacionItemRepository aprobaciones, PasoItemRepository pasos,
                                   EventoAprobacionProcesadoRepository procesados, MotorDeReglas motor, AuthApi auth,
                                   ApplicationEventPublisher eventos) {
        this.aprobaciones = aprobaciones;
        this.pasos = pasos;
        this.procesados = procesados;
        this.motor = motor;
        this.auth = auth;
        this.eventos = eventos;
    }

    // ---------------------------------------------------------------- eventos de otros módulos

    /** Por cada ítem de la solicitud crea su aprobación con los pasos que pide la regla que le toca. */
    @Transactional
    public void iniciar(SolicitudCreada e) {
        for (SolicitudCreada.ItemCreado i : e.items()) {
            // Idempotente: la clave natural es el id del ítem.
            if (aprobaciones.existsById(i.itemId())) {
                continue;
            }
            AprobacionItem item = new AprobacionItem();
            item.setItemId(i.itemId());
            item.setSolicitudId(e.solicitudId());
            item.setSolicitanteId(e.solicitanteId());
            item.setSectorId(e.sectorId());
            item.setCategoriaId(i.categoriaId());
            item.setTipo(i.tipo());
            item.setDetalle(i.detalle());
            item.setCantidad(i.cantidad());
            item.setUrgencia(e.urgencia());
            item.setPrecioEstimado(i.precioEstimado());
            item.setMonedaEstimada(i.monedaEstimada());
            item.setSolicitadoEn(e.ocurridoEn());

            ResultadoEvaluacion resultado = motor.evaluar(contexto(item));
            item.setReglaId(resultado.regla().getId());
            item.setReglaNombre(resultado.regla().getNombre());
            resultado.pasos().forEach(r -> item.agregarPaso(nuevoPaso(r)));

            if (resultado.pasos().isEmpty()) {
                item.setEstado(EstadoCadena.APROBADA);
                aprobaciones.save(item);
                publicarCompletada(item, true);
            } else {
                aprobaciones.saveAndFlush(item);
                item.pasosActivos().forEach(p -> publicarActivado(item, p));
            }
        }
    }

    /** Guarda el precio total y, si la cadena sigue abierta, la re-evalúa con el monto nuevo. */
    @Transactional
    public void precioActualizado(PrecioTotalActualizado e) {
        aprobaciones.findById(e.itemId()).ifPresent(item -> {
            item.setPrecioTotal(e.precioTotal());
            item.setMonedaTotal(e.precioTotal() == null ? null : e.moneda());
            item.setProveedorRazonSocial(e.precioTotal() == null ? null : e.proveedorRazonSocial());
            if (item.isCerrada() || item.getEstado() == EstadoCadena.RECHAZADA
                    || item.getEstado() == EstadoCadena.CANCELADA) {
                aprobaciones.save(item);
                return;
            }
            reevaluar(item, false, "PRECIO_CAMBIO");
        });
    }

    /**
     * El ítem se editó: se actualiza la copia y la cadena vuelve a empezar, porque el aprobador aprobó algo
     * distinto de lo que ahora se pide. No es idempotente por sí sola (reiniciaría decisiones nuevas), por eso
     * usa evento_procesado.
     */
    @Transactional
    public void itemEditado(ItemEditado e) {
        if (procesados.existsById(e.eventoId())) {
            return;
        }
        aprobaciones.findById(e.itemId()).ifPresent(item -> {
            if (item.isCerrada() || item.getEstado() == EstadoCadena.RECHAZADA
                    || item.getEstado() == EstadoCadena.CANCELADA) {
                return;
            }
            item.setCategoriaId(e.categoriaId());
            item.setCantidad(e.cantidad());
            item.setDetalle(e.detalle());
            item.setPrecioEstimado(e.precioEstimado());
            item.setMonedaEstimada(e.monedaEstimada());
            reevaluar(item, true, "EDICION");
        });
        procesados.save(new EventoAprobacionProcesado(e.eventoId()));
    }

    /** Ítem o solicitud cancelados: los pasos pendientes se cancelan y la aprobación queda CANCELADA. */
    @Transactional
    public void cancelar(List<Long> itemIds) {
        for (Long itemId : itemIds) {
            aprobaciones.findById(itemId).ifPresent(item -> {
                if (item.getEstado() == EstadoCadena.RECHAZADA || item.getEstado() == EstadoCadena.CANCELADA) {
                    return;
                }
                cancelarPendientes(item);
                item.setEstado(EstadoCadena.CANCELADA);
                aprobaciones.save(item);
            });
        }
    }

    /** Compras resolvió el ítem: si lo rechazó el Encargado se cancela la cadena; si se compró, se cierra. */
    @Transactional
    public void resuelto(ItemResuelto e) {
        aprobaciones.findById(e.itemId()).ifPresent(item -> {
            if ("COMPRADO".equals(e.resultado())) {
                item.setCerrada(true);
                aprobaciones.save(item);
            } else if ("RECHAZADO".equals(e.resultado()) && item.getEstado() == EstadoCadena.EN_CURSO) {
                cancelarPendientes(item);
                item.setEstado(EstadoCadena.CANCELADA);
                aprobaciones.save(item);
            }
            // Si ya estaba RECHAZADA (rechazó un aprobador), no hay nada que hacer.
        });
    }

    // ---------------------------------------------------------------- decisiones de los aprobadores

    @Transactional
    public void aprobar(Long pasoId, Integer version, Long usuarioId, Rol rol, Long sectorId) {
        PasoItem paso = validarDecision(pasoId, version, usuarioId, rol, sectorId);
        AprobacionItem item = paso.getItem();
        Set<Long> activosAntes = idsActivos(item);

        paso.setEstado(EstadoPaso.APROBADO);
        paso.setDecididoPor(usuarioId);
        paso.setDecididoEn(Instant.now());
        eventos.publishEvent(new PasoDecidido(UUID.randomUUID(), Instant.now(), item.getItemId(),
                item.getSolicitudId(), paso.getId(), "APROBADO", usuarioId, null));

        if (item.pasosActivos().isEmpty()) {
            item.setEstado(EstadoCadena.APROBADA);
            aprobaciones.save(item);
            publicarCompletada(item, false);
        } else {
            aprobaciones.save(item);
            publicarNuevosActivos(item, activosAntes);
        }
    }

    @Transactional
    public void rechazar(Long pasoId, String motivo, Integer version, Long usuarioId, Rol rol, Long sectorId) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("MOTIVO_REQUERIDO", "El rechazo requiere un motivo.");
        }
        PasoItem paso = validarDecision(pasoId, version, usuarioId, rol, sectorId);
        AprobacionItem item = paso.getItem();

        paso.setEstado(EstadoPaso.RECHAZADO);
        paso.setDecididoPor(usuarioId);
        paso.setDecididoEn(Instant.now());
        paso.setMotivo(motivo.trim());
        cancelarPendientes(item);
        item.setEstado(EstadoCadena.RECHAZADA);
        aprobaciones.save(item);
        // Compras escucha AprobacionRechazada y rechaza ese ítem (no la solicitud ni los demás ítems).
        eventos.publishEvent(new PasoDecidido(UUID.randomUUID(), Instant.now(), item.getItemId(),
                item.getSolicitudId(), paso.getId(), "RECHAZADO", usuarioId, paso.getMotivo()));
        eventos.publishEvent(new AprobacionRechazada(UUID.randomUUID(), Instant.now(), item.getItemId(),
                item.getSolicitudId(), paso.getId(), usuarioId, paso.getNombreAprobador(), paso.getMotivo()));
    }

    /** Las validaciones, en orden: existe y está en curso; está activo; lo puede decidir; no es suyo; versión. */
    private PasoItem validarDecision(Long pasoId, Integer version, Long usuarioId, Rol rol, Long sectorId) {
        PasoItem paso = pasos.findById(pasoId)
                .orElseThrow(() -> new NoEncontradoException("No existe el paso " + pasoId));
        AprobacionItem item = paso.getItem();
        if (item.getEstado() != EstadoCadena.EN_CURSO) {
            throw new ReglaNegocioException("APROBACION_CERRADA", "Este ítem ya no está en aprobación.");
        }
        if (paso.getEstado() != EstadoPaso.PENDIENTE || !item.pasosActivos().contains(paso)) {
            throw new ReglaNegocioException("PASO_NO_ACTIVO",
                    "Todavía no le toca a " + paso.getNombreAprobador() + ".");
        }
        if (!paso.puedeDecidir(rol, sectorId)) {
            throw new SinPermisoException("No podés decidir este paso.");
        }
        if (Objects.equals(item.getSolicitanteId(), usuarioId)) {
            throw new SinPermisoException("PEDIDO_PROPIO", "Nadie decide sobre su propio pedido.");
        }
        if (version == null || !version.equals(paso.getVersion())) {
            throw new ConflictoException("CONFLICTO_VERSION", "Otra persona decidió este paso. Recargá la página.");
        }
        return paso;
    }

    // ---------------------------------------------------------------- re-evaluación

    /**
     * Calcula de nuevo la cadena que pide el motor y la ajusta: un paso que ya existe se conserva con su estado
     * (y toma el orden nuevo), uno que falta se agrega PENDIENTE, un APROBADO que ya no se pide se conserva y un
     * PENDIENTE que ya no se pide pasa a OMITIDO. {@code reiniciar}: todos los pasos vuelven a PENDIENTE.
     */
    private void reevaluar(AprobacionItem item, boolean reiniciar, String motivoReapertura) {
        EstadoCadena antes = item.getEstado();
        if (reiniciar) {
            item.getPasos().stream().filter(p -> p.getEstado() != EstadoPaso.CANCELADO).forEach(p -> {
                p.setEstado(EstadoPaso.PENDIENTE);
                p.setDecididoPor(null);
                p.setDecididoEn(null);
                p.setMotivo(null);
            });
        }
        Set<Long> activosAntes = reiniciar ? new HashSet<>() : idsActivos(item);

        ResultadoEvaluacion resultado = motor.evaluar(contexto(item));
        Set<PasoItem> requeridos = new HashSet<>();
        for (PasoRequerido r : resultado.pasos()) {
            PasoItem existente = item.getPasos().stream()
                    .filter(p -> !requeridos.contains(p) && p.getEstado() != EstadoPaso.CANCELADO && mismoAprobador(p, r))
                    .findFirst().orElse(null);
            if (existente == null) {
                existente = nuevoPaso(r);
                item.agregarPaso(existente);
            } else {
                existente.setOrden(r.orden());
                if (existente.getEstado() == EstadoPaso.OMITIDO) {
                    existente.setEstado(EstadoPaso.PENDIENTE);
                }
            }
            requeridos.add(existente);
        }
        item.getPasos().stream()
                .filter(p -> !requeridos.contains(p) && p.getEstado() == EstadoPaso.PENDIENTE)
                .forEach(p -> p.setEstado(EstadoPaso.OMITIDO));
        item.setReglaId(resultado.regla().getId());
        item.setReglaNombre(resultado.regla().getNombre());
        aprobaciones.saveAndFlush(item);

        boolean hayPendientes = !item.pasosActivos().isEmpty();
        if (antes == EstadoCadena.APROBADA && hayPendientes) {
            item.setEstado(EstadoCadena.EN_CURSO);
            aprobaciones.save(item);
            eventos.publishEvent(new AprobacionReabierta(UUID.randomUUID(), Instant.now(), item.getItemId(),
                    item.getSolicitudId(), motivoReapertura));
        } else if (antes == EstadoCadena.EN_CURSO && !hayPendientes) {
            item.setEstado(EstadoCadena.APROBADA);
            aprobaciones.save(item);
            publicarCompletada(item, resultado.pasos().isEmpty());
        }
        if (item.getEstado() == EstadoCadena.EN_CURSO) {
            publicarNuevosActivos(item, activosAntes);
        }
    }

    private static boolean mismoAprobador(PasoItem p, PasoRequerido r) {
        return p.getTipoAprobador() == r.tipoAprobador()
                && (r.tipoAprobador() == TipoAprobador.ROL
                ? Objects.equals(p.getRol(), r.rol())
                : Objects.equals(p.getSectorId(), r.sectorId()));
    }

    // ---------------------------------------------------------------- utilidades

    private void cancelarPendientes(AprobacionItem item) {
        item.getPasos().stream().filter(p -> p.getEstado() == EstadoPaso.PENDIENTE)
                .forEach(p -> p.setEstado(EstadoPaso.CANCELADO));
    }

    private ContextoEvaluacion contexto(AprobacionItem item) {
        return new ContextoEvaluacion(monto(item), item.getSectorId(), item.getCategoriaId(),
                item.getSolicitanteId(), item.getUrgencia());
    }

    // Se evalúa con el precio total si ya existe; si no, con el estimado.
    private static Monto monto(AprobacionItem item) {
        return item.getPrecioTotal() != null
                ? new Monto(item.getPrecioTotal(), item.getMonedaTotal())
                : new Monto(item.getPrecioEstimado(), item.getMonedaEstimada());
    }

    private PasoItem nuevoPaso(PasoRequerido r) {
        PasoItem paso = new PasoItem();
        paso.setOrden(r.orden());
        paso.setTipoAprobador(r.tipoAprobador());
        paso.setRol(r.rol());
        paso.setSectorId(r.sectorId());
        // El motor deja un nombre provisorio para los sectores: el real sale de AuthApi.
        paso.setNombreAprobador(r.tipoAprobador() == TipoAprobador.SECTOR ? auth.nombreSector(r.sectorId()) : r.nombre());
        return paso;
    }

    private Set<Long> idsActivos(AprobacionItem item) {
        return item.pasosActivos().stream().map(PasoItem::getId).collect(Collectors.toSet());
    }

    private void publicarNuevosActivos(AprobacionItem item, Set<Long> activosAntes) {
        item.pasosActivos().stream().filter(p -> !activosAntes.contains(p.getId()))
                .forEach(p -> publicarActivado(item, p));
    }

    private void publicarActivado(AprobacionItem item, PasoItem paso) {
        Monto monto = monto(item);
        eventos.publishEvent(new PasoAprobacionActivado(UUID.randomUUID(), Instant.now(), item.getItemId(),
                item.getSolicitudId(), paso.getId(), paso.getTipoAprobador().name(), paso.getRol(), paso.getSectorId(),
                paso.getNombreAprobador(), monto.importe(), monto.moneda()));
    }

    private void publicarCompletada(AprobacionItem item, boolean automatica) {
        eventos.publishEvent(new AprobacionCompletada(UUID.randomUUID(), Instant.now(), item.getItemId(),
                item.getSolicitudId(), automatica));
    }
}
