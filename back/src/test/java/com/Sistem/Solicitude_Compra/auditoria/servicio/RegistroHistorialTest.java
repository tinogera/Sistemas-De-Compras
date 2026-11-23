package com.Sistem.Solicitude_Compra.auditoria.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EntradaHistorial;
import com.Sistem.Solicitude_Compra.auditoria.repositorio.HistorialRepository;
import com.Sistem.Solicitude_Compra.auditoria.repositorio.ItemSolicitudRepository;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionCompletada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemAsignado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.eventos.PasoDecidido;
import com.Sistem.Solicitude_Compra.compartido.eventos.ReglaModificada;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** T13.2: cada evento de cambio deja su fila en el historial, y repetirlo no la duplica. */
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class RegistroHistorialTest {

    private static final Long SOLICITUD = 10L;

    @BeforeAll
    static void configurarZonaHoraria() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired HistorialRepository historial;
    @Autowired ItemSolicitudRepository itemsSolicitud;
    @Autowired CompletedEventPublications completadas;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AuthApi authApi;
    @MockitoBean CatalogoApi catalogoApi;
    @MockitoBean UsuarioActual usuarioActual;

    @BeforeEach
    @AfterEach
    void limpiar() {
        // El historial no tiene delete a propósito: los tests limpian por SQL.
        jdbc.execute("TRUNCATE auditoria.historial, auditoria.evento_procesado, auditoria.item_solicitud");
    }

    private List<EntradaHistorial> de(EntidadHistorial entidad, Long id) {
        return historial.findByEntidadAndEntidadIdOrderByOcurridoEnAsc(entidad, id);
    }

    private SolicitudCreada solicitudCreada() {
        return new SolicitudCreada(UUID.randomUUID(), Instant.now(), SOLICITUD, 5L, 1L, Urgencia.ALTA,
                LocalDate.now().plusDays(3), List.of(
                        new SolicitudCreada.ItemCreado(101L, TipoItem.PRODUCTO, 7L, BigDecimal.ONE, "Resma",
                                new BigDecimal("100"), Moneda.ARS),
                        new SolicitudCreada.ItemCreado(102L, TipoItem.SERVICIO, 8L, BigDecimal.ONE, "Pelotero",
                                new BigDecimal("200"), Moneda.ARS)));
    }

    @Test
    void solicitudCreadaGuardaUnaEntradaYUnaFilaPorItem(Scenario scenario) {
        scenario.publish(solicitudCreada())
                .andWaitForStateChange(() -> de(EntidadHistorial.SOLICITUD, SOLICITUD), lista -> !lista.isEmpty())
                .andVerify(lista -> {
                    assertThat(lista).hasSize(1);
                    EntradaHistorial entrada = lista.get(0);
                    assertThat(entrada.getAccion()).isEqualTo(AccionHistorial.CREACION);
                    assertThat(entrada.getEstadoAnterior()).isNull();
                    assertThat(entrada.getEstadoNuevo()).isEqualTo("EN_REVISION");
                    assertThat(entrada.getUsuarioId()).isEqualTo(5L);
                    assertThat(itemsSolicitud.findBySolicitudId(SOLICITUD))
                            .extracting("itemId", "solicitanteId")
                            .containsExactlyInAnyOrder(
                                    org.assertj.core.groups.Tuple.tuple(101L, 5L),
                                    org.assertj.core.groups.Tuple.tuple(102L, 5L));
                });
    }

    @Test
    void publicarSolicitudCreadaDosVecesNoCambiaNada(Scenario scenario) {
        var evento = solicitudCreada();
        int previas = completadas.findAll().size();

        scenario.publish(evento)
                .andWaitForStateChange(() -> de(EntidadHistorial.SOLICITUD, SOLICITUD), lista -> !lista.isEmpty())
                .andVerify(lista -> assertThat(lista).hasSize(1));

        scenario.publish(evento)
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 2)
                .andVerify(c -> {
                    assertThat(de(EntidadHistorial.SOLICITUD, SOLICITUD)).hasSize(1);
                    assertThat(itemsSolicitud.findBySolicitudId(SOLICITUD)).hasSize(2);
                });
    }

    @Test
    void compraActualizadaDejaLosDosPrecios(Scenario scenario) {
        scenario.publish(new CompraActualizada(UUID.randomUUID(), Instant.now(), 101L, SOLICITUD,
                        new BigDecimal("100"), new BigDecimal("95"), null, new BigDecimal("10"), Moneda.ARS, 3L))
                .andWaitForStateChange(() -> de(EntidadHistorial.ITEM, 101L), lista -> !lista.isEmpty())
                .andVerify(lista -> {
                    assertThat(lista).hasSize(1);
                    assertThat(lista.get(0).getAccion()).isEqualTo(AccionHistorial.EDICION);
                    assertThat(lista.get(0).getUsuarioId()).isEqualTo(3L);
                    assertThat(lista.get(0).getComentario()).contains("100 → 95 ARS");
                });
    }

    @Test
    void unaAsignacionConEncargadoAnteriorEsUnaReasignacion(Scenario scenario) {
        scenario.publish(new ItemAsignado(UUID.randomUUID(), Instant.now(), 101L, SOLICITUD, 3L, 4L))
                .andWaitForStateChange(() -> de(EntidadHistorial.ITEM, 101L), lista -> !lista.isEmpty())
                .andVerify(lista -> {
                    assertThat(lista.get(0).getAccion()).isEqualTo(AccionHistorial.REASIGNACION);
                    assertThat(lista.get(0).getUsuarioId()).isEqualTo(4L);
                    assertThat(lista.get(0).getEstadoNuevo()).isEqualTo("EN_COTIZACION");
                });
    }

    @Test
    void laPrimeraAsignacionEsUnaAsignacion(Scenario scenario) {
        scenario.publish(new ItemAsignado(UUID.randomUUID(), Instant.now(), 101L, SOLICITUD, null, 4L))
                .andWaitForStateChange(() -> de(EntidadHistorial.ITEM, 101L), lista -> !lista.isEmpty())
                .andVerify(lista -> {
                    assertThat(lista.get(0).getAccion()).isEqualTo(AccionHistorial.ASIGNACION);
                    assertThat(lista.get(0).getEstadoAnterior()).isEqualTo("PENDIENTE");
                });
    }

    @Test
    void elRechazoDeUnPasoGuardaElNombreDelAprobadorYElMotivo(Scenario scenario) {
        when(authApi.obtenerUsuario(9L)).thenReturn(
                new UsuarioResumen(9L, "Gerencia General", "g@test.com", Rol.SUPERVISOR, 1L, true));

        scenario.publish(new PasoDecidido(UUID.randomUUID(), Instant.now(), 101L, SOLICITUD, 1L, "RECHAZADO", 9L,
                        "No hay presupuesto"))
                .andWaitForStateChange(() -> de(EntidadHistorial.ITEM, 101L), lista -> !lista.isEmpty())
                .andVerify(lista -> {
                    assertThat(lista.get(0).getAccion()).isEqualTo(AccionHistorial.RECHAZO);
                    assertThat(lista.get(0).getUsuarioId()).isEqualTo(9L);
                    assertThat(lista.get(0).getComentario()).contains("Gerencia General", "No hay presupuesto");
                });
    }

    @Test
    void laAprobacionAutomaticaQuedaSinUsuarioYMarcada(Scenario scenario) {
        scenario.publish(new AprobacionCompletada(UUID.randomUUID(), Instant.now(), 101L, SOLICITUD, true))
                .andWaitForStateChange(() -> de(EntidadHistorial.ITEM, 101L), lista -> !lista.isEmpty())
                .andVerify(lista -> {
                    assertThat(lista.get(0).getEstadoAnterior()).isEqualTo("EN_CURSO");
                    assertThat(lista.get(0).getEstadoNuevo()).isEqualTo("APROBADA");
                    assertThat(lista.get(0).getUsuarioId()).isNull();
                    assertThat(lista.get(0).getComentario()).isEqualTo("automática");
                });
    }

    @Test
    void modificarUnaReglaQuedaRegistrado(Scenario scenario) {
        scenario.publish(new ReglaModificada(UUID.randomUUID(), Instant.now(), 3L, "Compras grandes", "CREADA", 1L))
                .andWaitForStateChange(() -> de(EntidadHistorial.REGLA, 3L), lista -> !lista.isEmpty())
                .andVerify(lista -> {
                    assertThat(lista.get(0).getAccion()).isEqualTo(AccionHistorial.CREACION);
                    assertThat(lista.get(0).getUsuarioId()).isEqualTo(1L);
                });
    }

    @Test
    void itemResueltoComoCompradoNoSeDuplica(Scenario scenario) {
        int previas = completadas.findAll().size();

        scenario.publish(resuelto("COMPRADO"))
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 1)
                .andVerify(c -> assertThat(de(EntidadHistorial.ITEM, 101L)).isEmpty());
    }

    @Test
    void itemResueltoComoRechazadoQuedaConQuienYElMotivo(Scenario scenario) {
        scenario.publish(resuelto("RECHAZADO"))
                .andWaitForStateChange(() -> de(EntidadHistorial.ITEM, 101L), lista -> !lista.isEmpty())
                .andVerify(lista -> {
                    assertThat(lista.get(0).getEstadoNuevo()).isEqualTo("RECHAZADO");
                    assertThat(lista.get(0).getComentario()).contains("Gerencia General", "Caro");
                });
    }

    private ItemResuelto resuelto(String resultado) {
        return new ItemResuelto(UUID.randomUUID(), Instant.now(), 101L, SOLICITUD, resultado, null, "Caro",
                "Gerencia General", Urgencia.ALTA, Instant.now(), Instant.now(), 3L);
    }
}
