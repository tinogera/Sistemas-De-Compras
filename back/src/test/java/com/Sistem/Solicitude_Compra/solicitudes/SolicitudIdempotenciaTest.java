package com.Sistem.Solicitude_Compra.solicitudes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.ClaveIdempotenciaRepository;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.SolicitudRepository;
import com.Sistem.Solicitude_Compra.solicitudes.servicio.ResultadoCreacion;
import com.Sistem.Solicitude_Compra.solicitudes.servicio.SolicitudService;
import com.Sistem.Solicitude_Compra.solicitudes.web.CrearSolicitudRequest;
import com.Sistem.Solicitude_Compra.solicitudes.web.ItemRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Contra Postgres de verdad: la PRIMARY KEY es la que decide la carrera, un mock no la prueba. */
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class SolicitudIdempotenciaTest {

    @Autowired SolicitudService service;
    @Autowired SolicitudRepository solicitudes;
    @Autowired ClaveIdempotenciaRepository claves;
    @MockitoBean AuthApi authApi;
    @MockitoBean CatalogoApi catalogoApi;
    @MockitoBean UsuarioActual usuarioActual;

    @BeforeEach
    void preparar() {
        limpiar();
        when(authApi.sectorDe(1L)).thenReturn(1L);
        when(authApi.sectorDe(2L)).thenReturn(1L);
        when(catalogoApi.existeCategoria(5L)).thenReturn(true);
    }

    // La clave referencia a la solicitud: se borra primero.
    @AfterEach
    void limpiar() {
        claves.deleteAll();
        solicitudes.deleteAll();
    }

    @Test
    void elMismoPostDosVecesConLaMismaClaveCreaUnaSolaSolicitud() {
        UUID clave = UUID.randomUUID();

        ResultadoCreacion primera = service.crear(1L, request(), clave);
        ResultadoCreacion segunda = service.crear(1L, request(), clave);

        assertThat(primera.creada()).isTrue();
        assertThat(segunda.creada()).isFalse();
        assertThat(segunda.solicitud().getId()).isEqualTo(primera.solicitud().getId());
        assertThat(segunda.solicitud().getItems()).hasSize(1);
        assertThat(solicitudes.count()).isEqualTo(1);
    }

    @Test
    void conClavesDistintasSeCreanDosSolicitudes() {
        ResultadoCreacion primera = service.crear(1L, request(), UUID.randomUUID());
        ResultadoCreacion segunda = service.crear(1L, request(), UUID.randomUUID());

        assertThat(segunda.solicitud().getId()).isNotEqualTo(primera.solicitud().getId());
        assertThat(solicitudes.count()).isEqualTo(2);
    }

    @Test
    void sinClaveSeComportaComoAntes() {
        service.crear(1L, request(), null);
        service.crear(1L, request(), null);

        assertThat(solicitudes.count()).isEqualTo(2);
        assertThat(claves.count()).isZero();
    }

    @Test
    void laMismaClaveDeOtroUsuarioDa422YNoCreaNada() {
        UUID clave = UUID.randomUUID();
        service.crear(1L, request(), clave);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.crear(2L, request(), clave))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CLAVE_IDEMPOTENCIA_INVALIDA"));
        assertThat(solicitudes.count()).isEqualTo(1);
    }

    @Test
    void dosRequestsALaVezConLaMismaClaveDejanUnaSolaSolicitud() throws Exception {
        UUID clave = UUID.randomUUID();
        CyclicBarrier salida = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Object> intento = () -> {
                salida.await();
                try {
                    return service.crear(1L, request(), clave);
                } catch (Exception e) {
                    return e;
                }
            };
            List<Future<Object>> futuros = new ArrayList<>(List.of(pool.submit(intento), pool.submit(intento)));

            for (Future<Object> futuro : futuros) {
                Object resultado = futuro.get();
                // Según quién llegue primero, el segundo ve la clave (reintento) o choca con la PRIMARY KEY.
                assertThat(resultado).satisfiesAnyOf(
                        r -> assertThat(r).isInstanceOf(ResultadoCreacion.class),
                        r -> assertThat(r).isInstanceOf(DataIntegrityViolationException.class));
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(solicitudes.count()).isEqualTo(1);
        assertThat(claves.count()).isEqualTo(1);
    }

    private static CrearSolicitudRequest request() {
        return new CrearSolicitudRequest(Urgencia.ALTA, LocalDate.now().plusDays(4), "obs",
                List.of(new ItemRequest(TipoItem.SERVICIO, 5L, null, new BigDecimal("3"), "Pelotero",
                        new BigDecimal("180000"), Moneda.ARS)));
    }
}
