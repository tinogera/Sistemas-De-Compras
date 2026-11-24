package com.Sistem.Solicitude_Compra.compras.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.archivos.AlmacenamientoArchivos;
import com.Sistem.Solicitude_Compra.compartido.archivos.ValidadorArchivos;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.CotizacionRepository;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * T09.5 · Dos Encargados toman el mismo ítem al mismo tiempo, contra Postgres real. Sin @Transactional en el
 * test: cada hilo abre su propia transacción en el service, y el choque lo detecta el
 * UPDATE ... WHERE version = ? de la base. Tiene que ganar exactamente uno.
 */
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class GestionComprasConcurrenciaTest {

    private static final Long ITEM = 200L;
    private static final Long ENCARGADO_1 = 1L;
    private static final Long ENCARGADO_2 = 2L;

    @BeforeAll
    static void configurarZonaHoraria() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired GestionComprasService service;
    @Autowired GestionItemRepository gestiones;
    @Autowired CotizacionRepository cotizaciones;
    @MockitoBean AuthApi authApi;
    @MockitoBean CatalogoApi catalogoApi;
    @MockitoBean UsuarioActual usuarioActual;
    @MockitoBean ValidadorArchivos validadorArchivos;
    @MockitoBean AlmacenamientoArchivos almacenamientoArchivos;

    @BeforeEach
    @AfterEach
    void limpiar() {
        cotizaciones.deleteAll();
        gestiones.deleteAll();
    }

    @RepeatedTest(10)
    void dosEncargadosTomanElMismoItem_ganaSoloUno() throws Exception {
        gestiones.saveAndFlush(new GestionItem(ITEM, 10L, 2L, 5L, TipoItem.PRODUCTO, "Resma A4",
                new BigDecimal("3"), Urgencia.ALTA, LocalDate.now().plusDays(5), OffsetDateTime.now(),
                new BigDecimal("1500.00"), Moneda.ARS));

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<Resultado>> futuros = new ArrayList<>();
            for (Long encargado : List.of(ENCARGADO_1, ENCARGADO_2)) {
                futuros.add(hilos.submit(tomarCuandoSalga(salida, encargado)));
            }
            salida.countDown();

            List<Resultado> resultados = new ArrayList<>();
            for (Future<Resultado> futuro : futuros) {
                resultados.add(futuro.get(30, TimeUnit.SECONDS));
            }

            List<Resultado> ganadores = resultados.stream().filter(r -> r.error() == null).toList();
            List<Resultado> perdedores = resultados.stream().filter(r -> r.error() != null).toList();
            assertThat(ganadores).hasSize(1);
            assertThat(perdedores).hasSize(1);
            // El que pierde, o chocó por versión, o leyó después del commit del otro y recibió 409 YA_ASIGNADO.
            assertThat(perdedores.get(0).error())
                    .isInstanceOfAny(OptimisticLockingFailureException.class, ConflictoException.class);

            GestionItem guardado = gestiones.findById(ITEM).orElseThrow();
            assertThat(guardado.getEstado()).isEqualTo(EstadoGestion.EN_COTIZACION);
            assertThat(guardado.getEncargadoId()).isEqualTo(ganadores.get(0).encargadoId());
        } finally {
            hilos.shutdownNow();
        }
    }

    private Callable<Resultado> tomarCuandoSalga(CountDownLatch salida, Long encargadoId) {
        return () -> {
            salida.await();
            try {
                service.tomar(ITEM, encargadoId);
                return new Resultado(encargadoId, null);
            } catch (Exception e) {
                return new Resultado(encargadoId, e);
            }
        };
    }

    private record Resultado(Long encargadoId, Exception error) {
    }
}
