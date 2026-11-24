package com.Sistem.Solicitude_Compra.compras;

import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API pública del módulo Compras para los demás módulos (hoy: Integraciones, para los recordatorios). */
@Service
@Transactional(readOnly = true)
public class ComprasApi {

    static final Duration INTERVALO_ALTA = Duration.ofDays(3);
    static final Duration INTERVALO_MEDIA_Y_BAJA = Duration.ofDays(21);

    private final GestionItemRepository gestiones;

    public ComprasApi(GestionItemRepository gestiones) {
        this.gestiones = gestiones;
    }

    /**
     * Ítems PENDIENTES o EN_COTIZACION cuyo último recordatorio (o la fecha de solicitud, si nunca hubo uno)
     * más el intervalo de su urgencia ya pasó: 3 días para ALTA, 21 para MEDIA y BAJA.
     */
    public List<RecordatorioPendiente> recordatoriosPendientes(Instant ahora) {
        return gestiones.findByEstadoIn(List.of(EstadoGestion.PENDIENTE, EstadoGestion.EN_COTIZACION)).stream()
                .filter(g -> !desde(g).plus(intervalo(g.getUrgencia())).isAfter(ahora))
                .map(g -> new RecordatorioPendiente(g.getItemId(), g.getSolicitudId(), g.getDetalle(),
                        g.getUrgencia(), g.getFechaNecesaria(),
                        Duration.between(g.getSolicitadoEn().toInstant(), ahora).toDays(), g.getEncargadoId()))
                .toList();
    }

    @Transactional
    public void registrarRecordatorio(Long itemId, Instant cuando) {
        GestionItem gestion = gestiones.findById(itemId)
                .orElseThrow(() -> new NoEncontradoException("No existe el ítem " + itemId));
        gestion.registrarRecordatorio(cuando.atOffset(ZoneOffset.UTC));
        gestiones.save(gestion);
    }

    private static Instant desde(GestionItem g) {
        OffsetDateTime ultimo = g.getUltimoRecordatorioEn();
        return (ultimo != null ? ultimo : g.getSolicitadoEn()).toInstant();
    }

    private static Duration intervalo(Urgencia urgencia) {
        return urgencia == Urgencia.ALTA ? INTERVALO_ALTA : INTERVALO_MEDIA_Y_BAJA;
    }
}
