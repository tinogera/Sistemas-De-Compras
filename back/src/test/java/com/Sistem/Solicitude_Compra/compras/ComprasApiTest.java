package com.Sistem.Solicitude_Compra.compras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComprasApiTest {

    static final Instant AHORA = Instant.parse("2026-12-01T12:00:00Z");
    static final List<EstadoGestion> ABIERTOS = List.of(EstadoGestion.PENDIENTE, EstadoGestion.EN_COTIZACION);

    @Mock GestionItemRepository gestiones;

    private GestionItem gestion(long id, Urgencia urgencia, int diasDesdeSolicitud) {
        return new GestionItem(id, 10L, 1L, 1L, TipoItem.PRODUCTO, "item " + id, BigDecimal.ONE, urgencia, null,
                AHORA.minusSeconds(diasDesdeSolicitud * 86400L).atOffset(ZoneOffset.UTC), BigDecimal.TEN, Moneda.ARS);
    }

    private List<Long> ids(List<RecordatorioPendiente> r) {
        return r.stream().map(RecordatorioPendiente::itemId).toList();
    }

    @Test
    void altaCadaTresDiasMediaYBajaCadaTresSemanas() {
        when(gestiones.findByEstadoIn(ABIERTOS)).thenReturn(List.of(
                gestion(1, Urgencia.ALTA, 3), gestion(2, Urgencia.ALTA, 2),
                gestion(3, Urgencia.MEDIA, 21), gestion(4, Urgencia.MEDIA, 20),
                gestion(5, Urgencia.BAJA, 22), gestion(6, Urgencia.BAJA, 5)));

        assertEquals(List.of(1L, 3L, 5L), ids(new ComprasApi(gestiones).recordatoriosPendientes(AHORA)));
    }

    @Test
    void cuentaDesdeElUltimoRecordatorioSiLoHubo() {
        GestionItem g = gestion(1, Urgencia.ALTA, 10);
        g.registrarRecordatorio(AHORA.minusSeconds(86400L).atOffset(ZoneOffset.UTC));
        when(gestiones.findByEstadoIn(ABIERTOS)).thenReturn(List.of(g));
        ComprasApi api = new ComprasApi(gestiones);

        assertEquals(List.of(), api.recordatoriosPendientes(AHORA));
        assertEquals(List.of(1L), ids(api.recordatoriosPendientes(AHORA.plusSeconds(2 * 86400L))));
    }

    @Test
    void informaLosDiasSinResolverYElEncargado() {
        GestionItem g = gestion(1, Urgencia.ALTA, 4);
        g.tomar(8L);
        when(gestiones.findByEstadoIn(ABIERTOS)).thenReturn(List.of(g));

        RecordatorioPendiente r = new ComprasApi(gestiones).recordatoriosPendientes(AHORA).get(0);

        assertEquals(4, r.diasSinResolver());
        assertEquals(8L, r.encargadoId());
    }

    @Test
    void registrarElRecordatorioGuardaLaFecha() {
        GestionItem g = gestion(1, Urgencia.ALTA, 4);
        when(gestiones.findById(1L)).thenReturn(Optional.of(g));

        new ComprasApi(gestiones).registrarRecordatorio(1L, AHORA);

        assertEquals(AHORA, g.getUltimoRecordatorioEn().toInstant());
    }
}
