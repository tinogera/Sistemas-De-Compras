package com.Sistem.Solicitude_Compra.compras.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoCotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CotizacionRepositoryTest {

    @Autowired CotizacionRepository cotizaciones;
    @Autowired GestionItemRepository gestiones;
    @Autowired EntityManager em;

    private void gestion(Long itemId) {
        gestiones.saveAndFlush(new GestionItem(itemId, 1L, 1L, null, TipoItem.PRODUCTO, "Globos",
                new BigDecimal("3.00"), Urgencia.MEDIA, LocalDate.now().plusDays(10), OffsetDateTime.now(),
                new BigDecimal("100.00"), Moneda.ARS));
    }

    private Cotizacion cotizacion(Long itemId, OffsetDateTime cargadaEn) {
        return new Cotizacion(itemId, 5L, "Globos SA", Moneda.USD, new BigDecimal("10.00"),
                new BigDecimal("30.00"), LocalDate.now().plusDays(7), null, "obs", 9L, cargadaEn);
    }

    @Test
    void guardaYRecuperaTodosLosCampos() {
        gestion(100L);
        Cotizacion guardada = cotizaciones.saveAndFlush(cotizacion(100L, OffsetDateTime.now()));
        em.clear();

        Cotizacion leida = cotizaciones.findById(guardada.getId()).orElseThrow();

        assertThat(leida.getEstado()).isEqualTo(EstadoCotizacion.CANDIDATA);
        assertThat(leida.getMoneda()).isEqualTo(Moneda.USD);
        assertThat(leida.getPrecioTotal()).isEqualByComparingTo("30.00");
        assertThat(leida.getProveedorRazonSocial()).isEqualTo("Globos SA");
        assertThat(leida.getCargadaPor()).isEqualTo(9L);
    }

    @Test
    void listaLasDeUnItemDeLaMasNuevaALaMasVieja() {
        gestion(100L);
        gestion(200L);
        OffsetDateTime ahora = OffsetDateTime.now();
        Cotizacion vieja = cotizaciones.save(cotizacion(100L, ahora.minusHours(2)));
        Cotizacion nueva = cotizaciones.save(cotizacion(100L, ahora));
        cotizaciones.save(cotizacion(200L, ahora));
        cotizaciones.flush();

        List<Cotizacion> resultado = cotizaciones.findByItemIdOrderByCargadaEnDesc(100L);

        assertThat(resultado).extracting(Cotizacion::getId).containsExactly(nueva.getId(), vieja.getId());
    }

    @Test
    void unItemNoPuedeTenerDosSeleccionadas() {
        gestion(100L);
        Cotizacion a = cotizaciones.save(cotizacion(100L, OffsetDateTime.now()));
        Cotizacion b = cotizaciones.save(cotizacion(100L, OffsetDateTime.now()));
        cotizaciones.flush();
        em.createNativeQuery("update compras.cotizacion set estado = 'SELECCIONADA' where id = ?1")
                .setParameter(1, a.getId()).executeUpdate();

        assertThatThrownBy(() -> {
            em.createNativeQuery("update compras.cotizacion set estado = 'SELECCIONADA' where id = ?1")
                    .setParameter(1, b.getId()).executeUpdate();
            em.flush();
        }).hasMessageContaining("ux_una_seleccionada");
    }

    @Test
    void noSePuedeCotizarUnItemQueNoTieneGestion() {
        assertThatThrownBy(() -> cotizaciones.saveAndFlush(cotizacion(999L, OffsetDateTime.now())))
                .hasMessageContaining("fk_cotizacion_item");
    }
}
