package com.Sistem.Solicitude_Compra.aprobaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.AprobacionItem;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoCadena;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoPaso;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.PasoItem;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.AprobacionItemRepository;
import com.Sistem.Solicitude_Compra.aprobaciones.servicios.CadenaAprobacionService;
import com.Sistem.Solicitude_Compra.aprobaciones.servicios.ReglaAdminService;
import com.Sistem.Solicitude_Compra.aprobaciones.web.ReglaRequest;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.compartido.eventos.PasoAprobacionActivado;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

/** T23.4 y T23.5 con la base real: los eventos de verdad, la idempotencia, los endpoints y la concurrencia. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, CadenaAprobacionIntegracionTest.Recolector.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class CadenaAprobacionIntegracionTest {

    private static final Long SECTOR_GG = 5L;
    private static final Long SOLICITANTE = 100L;
    private static final long ITEM = 9001L;

    @TestConfiguration
    static class Recolector {
        final List<PasoAprobacionActivado> activados = new CopyOnWriteArrayList<>();

        @EventListener
        void alActivarse(PasoAprobacionActivado e) {
            activados.add(e);
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ApplicationEventPublisher publicador;
    @Autowired TransactionTemplate tx;
    @Autowired AprobacionItemRepository aprobaciones;
    @Autowired CadenaAprobacionService cadena;
    @Autowired ReglaAdminService reglas;
    @Autowired JdbcTemplate jdbc;
    @Autowired Recolector recolector;
    @MockitoBean AuthApi auth;

    @BeforeEach
    void preparar() {
        limpiar();
        recolector.activados.clear();
        when(auth.nombreSector(SECTOR_GG)).thenReturn("Gerencia General");
        when(auth.existeSector(SECTOR_GG)).thenReturn(true);
        when(auth.hayUsuariosActivosConRol(Rol.SUPERVISOR)).thenReturn(true);
        when(auth.hayUsuariosActivosEnSector(SECTOR_GG)).thenReturn(true);
        reglas.crear(new ReglaRequest("Compras grandes", 10, true,
                List.of(new ReglaRequest.CondicionRequest(
                        com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoCondicion.MONTO,
                        com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.OperadorCondicion.MAYOR,
                        new BigDecimal("500000"), Moneda.ARS)),
                List.of(new ReglaRequest.PasoRequest(1,
                                com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador.ROL,
                                Rol.SUPERVISOR, null),
                        new ReglaRequest.PasoRequest(2,
                                com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador.SECTOR,
                                null, SECTOR_GG)),
                null), 1L);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM aprobaciones.paso_item WHERE item_id >= 9000");
        jdbc.update("DELETE FROM aprobaciones.aprobacion_item WHERE item_id >= 9000");
        jdbc.update("DELETE FROM compras.gestion_item WHERE item_id >= 9000");
        jdbc.update("DELETE FROM aprobaciones.regla_condicion WHERE regla_id IN "
                + "(SELECT id FROM aprobaciones.regla WHERE NOT por_defecto)");
        jdbc.update("DELETE FROM aprobaciones.regla_paso WHERE regla_id IN "
                + "(SELECT id FROM aprobaciones.regla WHERE NOT por_defecto)");
        jdbc.update("DELETE FROM aprobaciones.regla WHERE NOT por_defecto");
    }

    private SolicitudCreada solicitudPor(String importe) {
        var item = new SolicitudCreada.ItemCreado(ITEM, TipoItem.PRODUCTO, 3L, BigDecimal.ONE, "Equipo",
                new BigDecimal(importe), Moneda.ARS);
        return new SolicitudCreada(UUID.randomUUID(), Instant.now(), 8001L, SOLICITANTE, 1L, Urgencia.ALTA,
                LocalDate.now().plusDays(5), List.of(item));
    }

    private static void esperar(BooleanSupplier condicion) throws InterruptedException {
        for (int i = 0; i < 100 && !condicion.getAsBoolean(); i++) {
            Thread.sleep(100);
        }
        assertThat(condicion.getAsBoolean()).as("la condición no se cumplió a tiempo").isTrue();
    }

    private AprobacionItem cargar() {
        return tx.execute(s -> {
            AprobacionItem item = aprobaciones.findById(ITEM).orElseThrow();
            item.getPasos().size();
            return item;
        });
    }

    private static RequestPostProcessor como(Long id, String rol, long sector) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("rol", rol).claim("sectorId", sector))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Test
    void unaSolicitudPor600000GeneraDosPasosYUnSoloAvisoAunSiElEventoLlegaDosVeces() throws Exception {
        SolicitudCreada evento = solicitudPor("600000");

        tx.executeWithoutResult(s -> publicador.publishEvent(evento));
        tx.executeWithoutResult(s -> publicador.publishEvent(evento));
        esperar(() -> aprobaciones.existsById(ITEM));
        Thread.sleep(1500); // deja que se procese el segundo envío

        AprobacionItem item = cargar();
        assertThat(item.getPasos()).hasSize(2);
        assertThat(item.getReglaNombre()).isEqualTo("Compras grandes");
        assertThat(recolector.activados).hasSize(1);
        assertThat(recolector.activados.get(0).aprobadorNombre()).isEqualTo("Supervisor");
    }

    @Test
    void conMontoChicoLaReglaPorDefectoPideSoloAlSupervisor() throws Exception {
        tx.executeWithoutResult(s -> publicador.publishEvent(solicitudPor("30000")));
        esperar(() -> aprobaciones.existsById(ITEM));

        assertThat(cargar().getPasos()).extracting(PasoItem::getNombreAprobador).containsExactly("Supervisor");
    }

    @Test
    void elEndpointAprueba200YLosErroresTienenSuCodigo() throws Exception {
        cadena.iniciar(solicitudPor("600000"));
        AprobacionItem item = cargar();
        PasoItem primero = item.getPasos().get(0);
        PasoItem segundo = item.getPasos().get(1);
        String cuerpo = "{ \"version\": " + primero.getVersion() + " }";

        // El dueño del pedido no puede decidir.
        mockMvc.perform(post("/api/v1/aprobaciones/pasos/{id}/aprobar", primero.getId())
                        .with(como(SOLICITANTE, "SUPERVISOR", 1)).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("PEDIDO_PROPIO"));
        // A Gerencia General todavía no le toca.
        mockMvc.perform(post("/api/v1/aprobaciones/pasos/{id}/aprobar", segundo.getId())
                        .with(como(300L, "SOLICITANTE", SECTOR_GG)).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"version\": " + segundo.getVersion() + " }"))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("PASO_NO_ACTIVO"));
        // Sin la versión es un pedido mal formado.
        mockMvc.perform(post("/api/v1/aprobaciones/pasos/{id}/aprobar", primero.getId())
                        .with(como(200L, "SUPERVISOR", 1)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        // El Supervisor aprueba.
        mockMvc.perform(post("/api/v1/aprobaciones/pasos/{id}/aprobar", primero.getId())
                        .with(como(200L, "SUPERVISOR", 1)).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk());

        AprobacionItem despues = cargar();
        assertThat(despues.getPasos().get(0).getEstado()).isEqualTo(EstadoPaso.APROBADO);
        assertThat(despues.getEstado()).isEqualTo(EstadoCadena.EN_CURSO);
        // Se avisó al Supervisor al crear la cadena y a Gerencia General al aprobar el primero.
        assertThat(recolector.activados).extracting(PasoAprobacionActivado::aprobadorNombre)
                .containsExactly("Supervisor", "Gerencia General");
    }

    @Test
    void rechazarPorLaApiDejaLaAprobacionRechazadaConSuMotivo() throws Exception {
        cadena.iniciar(solicitudPor("600000"));
        PasoItem primero = cargar().getPasos().get(0);

        mockMvc.perform(post("/api/v1/aprobaciones/pasos/{id}/rechazar", primero.getId())
                        .with(como(200L, "SUPERVISOR", 1)).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"motivo\": \"Muy caro\", \"version\": " + primero.getVersion() + " }"))
                .andExpect(status().isOk());

        AprobacionItem item = cargar();
        assertThat(item.getEstado()).isEqualTo(EstadoCadena.RECHAZADA);
        assertThat(item.getPasos()).extracting(PasoItem::getEstado)
                .containsExactly(EstadoPaso.RECHAZADO, EstadoPaso.CANCELADO);
    }

    @Test
    void dosHilosAprobandoElMismoPasoSoloUnoGana() throws Exception {
        cadena.iniciar(solicitudPor("600000"));
        PasoItem primero = cargar().getPasos().get(0);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> resultados = List.of(
                    pool.submit(() -> intentarAprobar(salida, primero)),
                    pool.submit(() -> intentarAprobar(salida, primero)));
            salida.countDown();
            long ganadores = 0;
            for (Future<Boolean> f : resultados) {
                if (f.get()) {
                    ganadores++;
                }
            }
            assertThat(ganadores).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(cargar().getPasos().get(0).getEstado()).isEqualTo(EstadoPaso.APROBADO);
    }

    private boolean intentarAprobar(CountDownLatch salida, PasoItem paso) throws InterruptedException {
        salida.await();
        try {
            cadena.aprobar(paso.getId(), paso.getVersion(), 200L, Rol.SUPERVISOR, 1L);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
