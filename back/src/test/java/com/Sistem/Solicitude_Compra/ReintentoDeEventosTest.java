package com.Sistem.Solicitude_Compra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraRegistrada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.ordenes.repositorio.OrdenPedidoRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * T12.1. Un evento cuyo listener falla queda registrado como pendiente y se procesa cuando se reintenta;
 * con completion-mode=delete, al procesarse se borra de la tabla.
 * El caso real: CompraActualizada llega antes que CompraRegistrada, así que todavía no existe la orden.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8",
        // Igual que en producción (en el resto de los tests es "update", ver src/test/resources/config).
        "spring.modulith.events.completion-mode=delete"})
class ReintentoDeEventosTest {

    private static final Long ITEM = 7101L;

    @Autowired ApplicationEventPublisher eventos;
    @Autowired TransactionTemplate transacciones;
    @Autowired IncompleteEventPublications incompletas;
    @Autowired OrdenPedidoRepository ordenes;
    @Autowired JdbcTemplate jdbc;
    @Value("${spring.modulith.events.republish-outstanding-events-on-restart}") boolean republicarAlReiniciar;

    @BeforeEach
    void preparar() {
        limpiar();
    }

    @AfterEach
    void limpiar() {
        ordenes.deleteAll();
        jdbc.update("delete from event_publication");
    }

    @Test
    void alReiniciarSeVuelvenAPublicarLosEventosPendientes() {
        assertThat(republicarAlReiniciar).isTrue();
    }

    @Test
    void unEventoQueFalloQuedaPendienteYSeProcesaAlReintentar() {
        // 1. La actualización llega antes que la compra: no hay orden, el listener falla y el evento queda pendiente.
        transacciones.executeWithoutResult(t -> eventos.publishEvent(compraActualizada()));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(publicaciones("CompraActualizada", "FAILED")).isEqualTo(1));
        assertThat(ordenes.existsByItemId(ITEM)).isFalse();

        // 2. Llega la compra y se genera la orden (con el precio viejo).
        transacciones.executeWithoutResult(t -> eventos.publishEvent(compraRegistrada()));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(ordenes.existsByItemId(ITEM)).isTrue());
        assertThat(ordenes.findByItemId(ITEM).orElseThrow().getPrecioTotal()).isEqualByComparingTo("1200.00");

        // 3. Se reintenta lo pendiente: ahora la actualización encuentra la orden y se aplica.
        incompletas.resubmitIncompletePublications(p -> p.getEvent() instanceof CompraActualizada);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(ordenes.findByItemId(ITEM).orElseThrow().getPrecioTotal()).isEqualByComparingTo("1500.00");
            // Procesado: la fila ya no está en la tabla (completion-mode=delete).
            assertThat(publicaciones("CompraActualizada")).isZero();
        });
    }

    private int publicaciones(String tipoDeEvento) {
        Integer cantidad = jdbc.queryForObject("select count(*) from event_publication where event_type like ?",
                Integer.class, "%" + tipoDeEvento);
        return cantidad == null ? 0 : cantidad;
    }

    private int publicaciones(String tipoDeEvento, String estado) {
        Integer cantidad = jdbc.queryForObject(
                "select count(*) from event_publication where event_type like ? and status = ?", Integer.class,
                "%" + tipoDeEvento, estado);
        return cantidad == null ? 0 : cantidad;
    }

    private static CompraActualizada compraActualizada() {
        return new CompraActualizada(UUID.randomUUID(), Instant.now(), ITEM, 10L, new BigDecimal("1200.00"),
                new BigDecimal("1500.00"), null, null, Moneda.ARS, 1L);
    }

    private static CompraRegistrada compraRegistrada() {
        return new CompraRegistrada(UUID.randomUUID(), Instant.now(), ITEM, 10L, 2L, 3L, TipoItem.PRODUCTO,
                "Resma A4", new BigDecimal("3"), 5L, "Papelera Central SA", null, Moneda.ARS,
                new BigDecimal("1200.00"), 1L);
    }
}
