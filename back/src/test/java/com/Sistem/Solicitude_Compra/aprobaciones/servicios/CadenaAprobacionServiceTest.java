package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.AprobacionItem;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoCadena;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoPaso;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.EventoAprobacionProcesado;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.PasoItem;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Regla;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.AprobacionItemRepository;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.EventoAprobacionProcesadoRepository;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.PasoItemRepository;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ErrorNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionCompletada;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionReabierta;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionRechazada;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemEditado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.eventos.PasoAprobacionActivado;
import com.Sistem.Solicitude_Compra.compartido.eventos.PasoDecidido;
import com.Sistem.Solicitude_Compra.compartido.eventos.PrecioTotalActualizado;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/** La cadena de aprobación sin Spring ni base de datos: el motor y los repositorios son simulados. */
@ExtendWith(MockitoExtension.class)
class CadenaAprobacionServiceTest {

    private static final Long SECTOR_GG = 5L;
    private static final Long SOLICITANTE = 100L;
    private static final Long SUPERVISOR = 200L;

    @Mock AprobacionItemRepository aprobaciones;
    @Mock PasoItemRepository pasos;
    @Mock EventoAprobacionProcesadoRepository procesados;
    @Mock MotorDeReglas motor;
    @Mock AuthApi auth;
    @Mock ApplicationEventPublisher publicador;

    final List<Object> publicados = new ArrayList<>();
    final AtomicLong ids = new AtomicLong(1000);
    CadenaAprobacionService cadena;

    @BeforeEach
    void preparar() {
        cadena = new CadenaAprobacionService(aprobaciones, pasos, procesados, motor, auth, publicador);
        lenient().doAnswer(i -> publicados.add(i.getArgument(0))).when(publicador).publishEvent(any(Object.class));
        // Como la base: al guardar, los pasos nuevos reciben su id.
        lenient().when(aprobaciones.saveAndFlush(any(AprobacionItem.class))).thenAnswer(i -> {
            AprobacionItem item = i.getArgument(0);
            item.getPasos().stream().filter(p -> p.getId() == null).forEach(p -> p.setId(ids.incrementAndGet()));
            return item;
        });
        lenient().when(aprobaciones.save(any(AprobacionItem.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(auth.nombreSector(SECTOR_GG)).thenReturn("Gerencia General");
    }

    // ---------------------------------------------------------------- armado de datos

    private static PasoRequerido supervisor(int orden) {
        return new PasoRequerido(orden, TipoAprobador.ROL, Rol.SUPERVISOR, null, "Supervisor");
    }

    private static PasoRequerido gerencia(int orden) {
        return new PasoRequerido(orden, TipoAprobador.SECTOR, null, SECTOR_GG, "Sector 5");
    }

    private void motorDevuelve(PasoRequerido... pasos) {
        Regla regla = new Regla();
        regla.setId(7L);
        regla.setNombre("Compras grandes");
        when(motor.evaluar(any())).thenReturn(new ResultadoEvaluacion(regla, List.of(pasos)));
    }

    private SolicitudCreada solicitudCreada(long... itemIds) {
        List<SolicitudCreada.ItemCreado> items = new ArrayList<>();
        for (long id : itemIds) {
            items.add(new SolicitudCreada.ItemCreado(id, TipoItem.PRODUCTO, 3L, BigDecimal.ONE, "Item " + id,
                    new BigDecimal("600000"), Moneda.ARS));
        }
        return new SolicitudCreada(UUID.randomUUID(), Instant.now(), 50L, SOLICITANTE, 1L, Urgencia.ALTA,
                LocalDate.now().plusDays(5), items);
    }

    private static PasoItem paso(Long id, int orden, TipoAprobador tipo, Rol rol, Long sector, String nombre,
                                 EstadoPaso estado) {
        PasoItem p = new PasoItem();
        p.setId(id);
        p.setOrden(orden);
        p.setTipoAprobador(tipo);
        p.setRol(rol);
        p.setSectorId(sector);
        p.setNombreAprobador(nombre);
        p.setEstado(estado);
        ReflectionTestUtils.setField(p, "version", 0);
        return p;
    }

    private static PasoItem pasoSupervisor(Long id, int orden, EstadoPaso estado) {
        return paso(id, orden, TipoAprobador.ROL, Rol.SUPERVISOR, null, "Supervisor", estado);
    }

    private static PasoItem pasoGerencia(Long id, int orden, EstadoPaso estado) {
        return paso(id, orden, TipoAprobador.SECTOR, null, SECTOR_GG, "Gerencia General", estado);
    }

    private AprobacionItem itemConPasos(EstadoCadena estado, PasoItem... pasosItem) {
        AprobacionItem item = new AprobacionItem();
        item.setItemId(1L);
        item.setSolicitudId(50L);
        item.setSolicitanteId(SOLICITANTE);
        item.setSectorId(1L);
        item.setCategoriaId(3L);
        item.setTipo(TipoItem.PRODUCTO);
        item.setCantidad(BigDecimal.ONE);
        item.setUrgencia(Urgencia.ALTA);
        item.setPrecioEstimado(new BigDecimal("600000"));
        item.setMonedaEstimada(Moneda.ARS);
        item.setSolicitadoEn(Instant.now());
        item.setEstado(estado);
        for (PasoItem p : pasosItem) {
            item.agregarPaso(p);
            lenient().when(pasos.findById(p.getId())).thenReturn(Optional.of(p));
        }
        lenient().when(aprobaciones.findById(1L)).thenReturn(Optional.of(item));
        return item;
    }

    private <T> List<T> publicados(Class<T> tipo) {
        return publicados.stream().filter(tipo::isInstance).map(tipo::cast).toList();
    }

    // ---------------------------------------------------------------- SolicitudCreada

    @Test
    void conDosPasosPublicaSoloElDelSupervisor() {
        motorDevuelve(supervisor(1), gerencia(2));
        List<AprobacionItem> guardados = new ArrayList<>();
        when(aprobaciones.saveAndFlush(any(AprobacionItem.class))).thenAnswer(i -> {
            AprobacionItem item = i.getArgument(0);
            item.getPasos().forEach(p -> p.setId(ids.incrementAndGet()));
            guardados.add(item);
            return item;
        });

        cadena.iniciar(solicitudCreada(1L));

        AprobacionItem item = guardados.get(0);
        assertThat(item.getEstado()).isEqualTo(EstadoCadena.EN_CURSO);
        assertThat(item.getReglaNombre()).isEqualTo("Compras grandes");
        assertThat(item.getPasos()).extracting(PasoItem::getNombreAprobador)
                .containsExactly("Supervisor", "Gerencia General");
        assertThat(publicados(PasoAprobacionActivado.class)).hasSize(1)
                .first().satisfies(e -> {
                    assertThat(e.aprobadorNombre()).isEqualTo("Supervisor");
                    assertThat(e.rol()).isEqualTo(Rol.SUPERVISOR);
                    assertThat(e.monto()).isEqualByComparingTo("600000");
                });
        assertThat(publicados(AprobacionCompletada.class)).isEmpty();
    }

    @Test
    void sinPasosQuedaAprobadaAutomaticamente() {
        motorDevuelve();

        cadena.iniciar(solicitudCreada(1L));

        assertThat(publicados(AprobacionCompletada.class)).singleElement()
                .satisfies(e -> assertThat(e.automatica()).isTrue());
        assertThat(publicados(PasoAprobacionActivado.class)).isEmpty();
    }

    @Test
    void siElItemYaTieneAprobacionNoHaceNada() {
        when(aprobaciones.existsById(1L)).thenReturn(true);

        cadena.iniciar(solicitudCreada(1L));

        assertThat(publicados).isEmpty();
    }

    // ---------------------------------------------------------------- PrecioTotalActualizado

    private PrecioTotalActualizado precio(String importe) {
        return new PrecioTotalActualizado(UUID.randomUUID(), Instant.now(), 1L, 50L, new BigDecimal(importe),
                Moneda.ARS, 9L, "Proveedor SA", 7L);
    }

    @Test
    void siSubeElPrecioSeAgregaElPasoYUnaAprobadaVuelveAEnCurso() {
        AprobacionItem item = itemConPasos(EstadoCadena.APROBADA, pasoSupervisor(1L, 1, EstadoPaso.APROBADO));
        motorDevuelve(supervisor(1), gerencia(2));

        cadena.precioActualizado(precio("900000"));

        assertThat(item.getEstado()).isEqualTo(EstadoCadena.EN_CURSO);
        assertThat(item.getPrecioTotal()).isEqualByComparingTo("900000");
        assertThat(item.getProveedorRazonSocial()).isEqualTo("Proveedor SA");
        assertThat(item.getPasos()).extracting(PasoItem::getEstado)
                .containsExactly(EstadoPaso.APROBADO, EstadoPaso.PENDIENTE);
        assertThat(publicados(AprobacionReabierta.class)).singleElement()
                .satisfies(e -> assertThat(e.motivo()).isEqualTo("PRECIO_CAMBIO"));
        assertThat(publicados(PasoAprobacionActivado.class)).singleElement()
                .satisfies(e -> assertThat(e.aprobadorNombre()).isEqualTo("Gerencia General"));
    }

    @Test
    void siBajaElPrecioUnPasoPendienteSeOmiteYSiNoQuedanPendientesSeCompleta() {
        AprobacionItem item = itemConPasos(EstadoCadena.EN_CURSO, pasoSupervisor(1L, 1, EstadoPaso.APROBADO),
                pasoGerencia(2L, 2, EstadoPaso.PENDIENTE));
        motorDevuelve(supervisor(1));

        cadena.precioActualizado(precio("100000"));

        assertThat(item.getPasos()).extracting(PasoItem::getEstado)
                .containsExactly(EstadoPaso.APROBADO, EstadoPaso.OMITIDO);
        assertThat(item.getEstado()).isEqualTo(EstadoCadena.APROBADA);
        assertThat(publicados(AprobacionCompletada.class)).singleElement()
                .satisfies(e -> assertThat(e.automatica()).isFalse());
    }

    @Test
    void unPasoAprobadoQueYaNoHaceFaltaSeConserva() {
        AprobacionItem item = itemConPasos(EstadoCadena.EN_CURSO, pasoSupervisor(1L, 1, EstadoPaso.APROBADO),
                pasoGerencia(2L, 2, EstadoPaso.PENDIENTE));
        // Ahora la regla pide solo a Gerencia General (en el orden 1).
        motorDevuelve(gerencia(1));

        cadena.precioActualizado(precio("100000"));

        assertThat(item.getPasos()).extracting(PasoItem::getNombreAprobador, PasoItem::getEstado, PasoItem::getOrden)
                .containsExactly(
                        tuple("Supervisor", EstadoPaso.APROBADO, 1),
                        tuple("Gerencia General", EstadoPaso.PENDIENTE, 1));
        assertThat(item.getEstado()).isEqualTo(EstadoCadena.EN_CURSO);
    }

    @Test
    void conLaCadenaCerradaSoloGuardaElPrecio() {
        AprobacionItem item = itemConPasos(EstadoCadena.APROBADA, pasoSupervisor(1L, 1, EstadoPaso.APROBADO));
        item.setCerrada(true);

        cadena.precioActualizado(precio("900000"));

        assertThat(item.getPrecioTotal()).isEqualByComparingTo("900000");
        assertThat(publicados).isEmpty();
    }

    @Test
    void conLaAprobacionRechazadaNoSeReevalua() {
        AprobacionItem item = itemConPasos(EstadoCadena.RECHAZADA, pasoSupervisor(1L, 1, EstadoPaso.RECHAZADO));

        cadena.precioActualizado(precio("900000"));

        assertThat(item.getEstado()).isEqualTo(EstadoCadena.RECHAZADA);
        assertThat(publicados).isEmpty();
    }

    // ---------------------------------------------------------------- ItemEditado

    private ItemEditado edicion() {
        return new ItemEditado(UUID.randomUUID(), Instant.now(), 1L, 50L, 4L, new BigDecimal("3"), "Nuevo detalle",
                new BigDecimal("700000"), Moneda.ARS, SOLICITANTE);
    }

    @Test
    void editarUnItemReiniciaLaCadenaYReabreLaAprobacion() {
        AprobacionItem item = itemConPasos(EstadoCadena.APROBADA, pasoSupervisor(1L, 1, EstadoPaso.APROBADO));
        item.getPasos().get(0).setDecididoPor(SUPERVISOR);
        motorDevuelve(supervisor(1));

        cadena.itemEditado(edicion());

        assertThat(item.getDetalle()).isEqualTo("Nuevo detalle");
        assertThat(item.getPrecioEstimado()).isEqualByComparingTo("700000");
        assertThat(item.getPasos().get(0).getEstado()).isEqualTo(EstadoPaso.PENDIENTE);
        assertThat(item.getPasos().get(0).getDecididoPor()).isNull();
        assertThat(item.getEstado()).isEqualTo(EstadoCadena.EN_CURSO);
        assertThat(publicados(AprobacionReabierta.class)).singleElement()
                .satisfies(e -> assertThat(e.motivo()).isEqualTo("EDICION"));
        assertThat(publicados(PasoAprobacionActivado.class)).hasSize(1);
    }

    @Test
    void elMismoEventoDeEdicionNoReiniciaDosVeces() {
        itemConPasos(EstadoCadena.EN_CURSO, pasoSupervisor(1L, 1, EstadoPaso.APROBADO),
                pasoGerencia(2L, 2, EstadoPaso.PENDIENTE));
        ItemEditado evento = edicion();
        when(procesados.existsById(evento.eventoId())).thenReturn(true);

        cadena.itemEditado(evento);

        assertThat(publicados).isEmpty();
    }

    // ---------------------------------------------------------------- cancelación y resolución

    @Test
    void cancelarPasaLosPasosPendientesACanceladoYLaAprobacionACancelada() {
        AprobacionItem item = itemConPasos(EstadoCadena.EN_CURSO, pasoSupervisor(1L, 1, EstadoPaso.APROBADO),
                pasoGerencia(2L, 2, EstadoPaso.PENDIENTE));

        cadena.cancelar(List.of(1L));

        assertThat(item.getEstado()).isEqualTo(EstadoCadena.CANCELADA);
        assertThat(item.getPasos()).extracting(PasoItem::getEstado)
                .containsExactly(EstadoPaso.APROBADO, EstadoPaso.CANCELADO);
    }

    private ItemResuelto resuelto(String resultado) {
        return new ItemResuelto(UUID.randomUUID(), Instant.now(), 1L, 50L, resultado, null, null, null,
                Urgencia.ALTA, Instant.now(), Instant.now(), null);
    }

    @Test
    void siElEncargadoRechazaLaCadenaEnCursoSeCancela() {
        AprobacionItem item = itemConPasos(EstadoCadena.EN_CURSO, pasoSupervisor(1L, 1, EstadoPaso.PENDIENTE));

        cadena.resuelto(resuelto("RECHAZADO"));

        assertThat(item.getEstado()).isEqualTo(EstadoCadena.CANCELADA);
        assertThat(item.getPasos().get(0).getEstado()).isEqualTo(EstadoPaso.CANCELADO);
    }

    @Test
    void siRechazoUnAprobadorElRechazoDeComprasNoCambiaNada() {
        AprobacionItem item = itemConPasos(EstadoCadena.RECHAZADA, pasoSupervisor(1L, 1, EstadoPaso.RECHAZADO));

        cadena.resuelto(resuelto("RECHAZADO"));

        assertThat(item.getEstado()).isEqualTo(EstadoCadena.RECHAZADA);
    }

    @Test
    void alComprarseLaCadenaSeCierra() {
        AprobacionItem item = itemConPasos(EstadoCadena.APROBADA, pasoSupervisor(1L, 1, EstadoPaso.APROBADO));

        cadena.resuelto(resuelto("COMPRADO"));

        assertThat(item.isCerrada()).isTrue();
    }

    // ---------------------------------------------------------------- aprobar y rechazar

    private AprobacionItem cadenaDeDosPasos() {
        return itemConPasos(EstadoCadena.EN_CURSO, pasoSupervisor(1L, 1, EstadoPaso.PENDIENTE),
                pasoGerencia(2L, 2, EstadoPaso.PENDIENTE));
    }

    @Test
    void aprobarElPrimeroActivaAlSegundoConUnSoloAviso() {
        AprobacionItem item = cadenaDeDosPasos();

        cadena.aprobar(1L, item.getPasos().get(0).getVersion(), SUPERVISOR, Rol.SUPERVISOR, 9L);

        assertThat(item.getPasos().get(0).getEstado()).isEqualTo(EstadoPaso.APROBADO);
        assertThat(item.getPasos().get(0).getDecididoPor()).isEqualTo(SUPERVISOR);
        assertThat(item.getEstado()).isEqualTo(EstadoCadena.EN_CURSO);
        assertThat(publicados(PasoDecidido.class)).singleElement()
                .satisfies(e -> assertThat(e.decision()).isEqualTo("APROBADO"));
        assertThat(publicados(PasoAprobacionActivado.class)).singleElement()
                .satisfies(e -> assertThat(e.aprobadorNombre()).isEqualTo("Gerencia General"));
    }

    @Test
    void aprobarElUltimoCompletaLaAprobacion() {
        AprobacionItem item = itemConPasos(EstadoCadena.EN_CURSO, pasoSupervisor(1L, 1, EstadoPaso.APROBADO),
                pasoGerencia(2L, 2, EstadoPaso.PENDIENTE));

        cadena.aprobar(2L, item.getPasos().get(1).getVersion(), 300L, Rol.SOLICITANTE, SECTOR_GG);

        assertThat(item.getEstado()).isEqualTo(EstadoCadena.APROBADA);
        assertThat(publicados(AprobacionCompletada.class)).singleElement()
                .satisfies(e -> assertThat(e.automatica()).isFalse());
        assertThat(publicados(PasoAprobacionActivado.class)).isEmpty();
    }

    @Test
    void rechazarCancelaLosPendientesYPublicaElRechazo() {
        AprobacionItem item = cadenaDeDosPasos();

        cadena.rechazar(1L, "Es muy caro", item.getPasos().get(0).getVersion(), SUPERVISOR, Rol.SUPERVISOR, 9L);

        assertThat(item.getEstado()).isEqualTo(EstadoCadena.RECHAZADA);
        assertThat(item.getPasos()).extracting(PasoItem::getEstado)
                .containsExactly(EstadoPaso.RECHAZADO, EstadoPaso.CANCELADO);
        assertThat(item.getPasos().get(0).getMotivo()).isEqualTo("Es muy caro");
        assertThat(publicados(AprobacionRechazada.class)).singleElement().satisfies(e -> {
            assertThat(e.motivo()).isEqualTo("Es muy caro");
            assertThat(e.aprobadorNombre()).isEqualTo("Supervisor");
            assertThat(e.usuarioId()).isEqualTo(SUPERVISOR);
        });
        assertThat(publicados(PasoDecidido.class)).singleElement()
                .satisfies(e -> assertThat(e.decision()).isEqualTo("RECHAZADO"));
    }

    @Test
    void elRechazoSinMotivoEs422() {
        cadenaDeDosPasos();

        assertThatThrownBy(() -> cadena.rechazar(1L, "  ", 0, SUPERVISOR, Rol.SUPERVISOR, 9L))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(e -> ((ErrorNegocioException) e).getCodigo()).isEqualTo("MOTIVO_REQUERIDO");
    }

    @Test
    void elPasoInexistenteEs404() {
        when(pasos.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cadena.aprobar(99L, 0, SUPERVISOR, Rol.SUPERVISOR, 9L))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void siLaAprobacionYaNoEstaEnCursoEs422() {
        itemConPasos(EstadoCadena.RECHAZADA, pasoSupervisor(1L, 1, EstadoPaso.PENDIENTE));

        assertThatThrownBy(() -> cadena.aprobar(1L, 0, SUPERVISOR, Rol.SUPERVISOR, 9L))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(e -> ((ErrorNegocioException) e).getCodigo()).isEqualTo("APROBACION_CERRADA");
    }

    @Test
    void gerenciaNoPuedeDecidirAntesQueElSupervisor() {
        cadenaDeDosPasos();

        assertThatThrownBy(() -> cadena.aprobar(2L, 0, 300L, Rol.SOLICITANTE, SECTOR_GG))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Todavía no le toca a Gerencia General")
                .extracting(e -> ((ErrorNegocioException) e).getCodigo()).isEqualTo("PASO_NO_ACTIVO");
    }

    @Test
    void unUsuarioDeOtroSectorOConOtroRolNoPuedeDecidir() {
        cadenaDeDosPasos();

        assertThatThrownBy(() -> cadena.aprobar(1L, 0, 300L, Rol.ENCARGADO, 9L))
                .isInstanceOf(SinPermisoException.class)
                .extracting(e -> ((ErrorNegocioException) e).getCodigo()).isEqualTo("SIN_PERMISO");
    }

    @Test
    void nadieDecideSobreSuPropioPedido() {
        cadenaDeDosPasos();

        assertThatThrownBy(() -> cadena.aprobar(1L, 0, SOLICITANTE, Rol.SUPERVISOR, 9L))
                .isInstanceOf(SinPermisoException.class)
                .extracting(e -> ((ErrorNegocioException) e).getCodigo()).isEqualTo("PEDIDO_PROPIO");
    }

    @Test
    void conUnaVersionVencidaEs409() {
        AprobacionItem item = cadenaDeDosPasos();
        ReflectionTestUtils.setField(item.getPasos().get(0), "version", 3);

        assertThatThrownBy(() -> cadena.aprobar(1L, 2, SUPERVISOR, Rol.SUPERVISOR, 9L))
                .isInstanceOf(ConflictoException.class);
    }
}
