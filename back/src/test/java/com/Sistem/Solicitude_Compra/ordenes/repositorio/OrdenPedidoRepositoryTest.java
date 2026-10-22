package com.Sistem.Solicitude_Compra.ordenes.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.ordenes.dominio.EstadoOrden;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class OrdenPedidoRepositoryTest {

    @Autowired
    OrdenPedidoRepository repositorio;

    @Autowired
    EntityManager em;

    @Test
    void laOrdenNuevaQuedaGeneradaYSinEnvio() {
        OrdenPedido orden = repositorio.saveAndFlush(orden(1L, "1000.00", "2"));
        em.clear();

        OrdenPedido guardada = repositorio.findById(orden.getId()).orElseThrow();

        assertThat(guardada.getEstado()).isEqualTo(EstadoOrden.GENERADA);
        assertThat(guardada.getEmitidaEn()).isNotNull();
        assertThat(guardada.getMontoEnvio()).isNull();
        assertThat(guardada.getEnviadaPor()).isNull();
        assertThat(guardada.getEnviadaEn()).isNull();
    }

    @Test
    void elPrecioUnitarioSeRedondeaADosDecimales() {
        OrdenPedido orden = repositorio.saveAndFlush(orden(1L, "1000.00", "3"));
        em.clear();

        OrdenPedido guardada = repositorio.findById(orden.getId()).orElseThrow();

        assertThat(guardada.getPrecioUnitario()).isEqualByComparingTo("333.33");
    }

    @Test
    void existsByItemIdDistingueEntreItems() {
        repositorio.saveAndFlush(orden(10L, "100.00", "1"));

        assertThat(repositorio.existsByItemId(10L)).isTrue();
        assertThat(repositorio.existsByItemId(11L)).isFalse();
    }

    @Test
    void noSePuedenGuardarDosOrdenesParaElMismoItem() {
        repositorio.saveAndFlush(orden(20L, "100.00", "1"));

        assertThatThrownBy(() -> repositorio.saveAndFlush(orden(20L, "200.00", "1")))
                .isInstanceOfAny(PersistenceException.class,
                        org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void siguienteNumeroDevuelveValoresCrecientes() {
        Long primero = repositorio.siguienteNumero();
        Long segundo = repositorio.siguienteNumero();

        assertThat(primero).isNotNull();
        assertThat(segundo).isGreaterThan(primero);
    }

    @Test
    void findByEstadoFiltraPorEstado() {
        OrdenPedido enviada = repositorio.saveAndFlush(orden(30L, "100.00", "1"));
        repositorio.saveAndFlush(orden(31L, "100.00", "1"));
        repositorio.saveAndFlush(orden(32L, "100.00", "1"));
        em.createNativeQuery("update ordenes.orden_pedido set estado = 'ENVIADA' where id = :id")
                .setParameter("id", enviada.getId())
                .executeUpdate();
        em.clear();

        var generadas = repositorio.findByEstado(EstadoOrden.GENERADA, PageRequest.of(0, 10));
        var enviadas = repositorio.findByEstado(EstadoOrden.ENVIADA, PageRequest.of(0, 10));

        assertThat(generadas.getTotalElements()).isEqualTo(2);
        assertThat(enviadas.getTotalElements()).isEqualTo(1);
    }

    @Test
    void actualizarMontosCorrigeTotalEnvioYUnitario() {
        OrdenPedido orden = repositorio.saveAndFlush(orden(40L, "1000.00", "2"));

        orden.actualizarMontos(new BigDecimal("600.00"), new BigDecimal("50.00"));
        repositorio.saveAndFlush(orden);
        em.clear();

        OrdenPedido guardada = repositorio.findById(orden.getId()).orElseThrow();
        assertThat(guardada.getPrecioTotal()).isEqualByComparingTo("600.00");
        assertThat(guardada.getMontoEnvio()).isEqualByComparingTo("50.00");
        assertThat(guardada.getPrecioUnitario()).isEqualByComparingTo("300.00");
    }

    @Test
    void actualizarMontosRechazaPrecioInvalidoYEnvioNegativo() {
        OrdenPedido orden = orden(50L, "1000.00", "2");

        assertThatThrownBy(() -> orden.actualizarMontos(BigDecimal.ZERO, null))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> orden.actualizarMontos(null, null))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> orden.actualizarMontos(new BigDecimal("100.00"), new BigDecimal("-1")))
                .isInstanceOf(ReglaNegocioException.class);
    }

    private OrdenPedido orden(Long itemId, String precioTotal, String cantidad) {
        return new OrdenPedido(
                repositorio.siguienteNumero(),
                itemId,
                1L,
                1L,
                "Proveedor SA",
                null,
                TipoItem.PRODUCTO,
                "Notebooks",
                new BigDecimal(cantidad),
                Moneda.ARS,
                new BigDecimal(precioTotal));
    }
}
