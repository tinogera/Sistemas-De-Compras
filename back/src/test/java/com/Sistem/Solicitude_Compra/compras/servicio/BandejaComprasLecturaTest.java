package com.Sistem.Solicitude_Compra.compras.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.catalogo.ProveedorResumen;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.CotizacionRepository;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import com.Sistem.Solicitude_Compra.compras.web.BandejaItemResponse;
import com.Sistem.Solicitude_Compra.compras.web.GestionItemDetalleResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

/** T10.6 y T10.5 contra la base: la bandeja marca las cotizaciones vencidas con una consulta por página. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BandejaComprasLecturaTest {

    private static final LocalDate HOY = LocalDate.now(Cotizacion.ZONA);

    @Autowired GestionItemRepository gestiones;
    @Autowired CotizacionRepository cotizaciones;

    private CatalogoApi catalogo;
    private BandejaComprasService service;

    @BeforeEach
    void iniciar() {
        catalogo = mock(CatalogoApi.class);
        AuthApi auth = mock(AuthApi.class);
        when(catalogo.nombreCategoria(5L)).thenReturn("Papelería");
        service = new BandejaComprasService(gestiones, cotizaciones, catalogo, auth);
    }

    private void item(Long id) {
        gestiones.saveAndFlush(new GestionItem(id, 10L, 2L, 5L, TipoItem.PRODUCTO, "Item " + id,
                new BigDecimal("2"), Urgencia.ALTA, HOY.plusDays(5), OffsetDateTime.now(),
                new BigDecimal("1000.00"), Moneda.ARS));
    }

    private void cotizacion(Long itemId, LocalDate validez) {
        cotizaciones.saveAndFlush(new Cotizacion(itemId, 9L, "Papelera SA", Moneda.ARS, new BigDecimal("10.00"),
                new BigDecimal("20.00"), validez, null, null, 1L, OffsetDateTime.now()));
    }

    @Test
    void marcaTodasVencidasSoloSiHayCotizacionesYNingunaVigente() {
        item(1L); // dos vencidas
        cotizacion(1L, HOY.minusDays(1));
        cotizacion(1L, HOY.minusDays(10));
        item(2L); // una vencida y una vigente
        cotizacion(2L, HOY.minusDays(1));
        cotizacion(2L, HOY.plusDays(3));
        item(3L); // sin cotizaciones
        item(4L); // vence hoy: todavía vigente
        cotizacion(4L, HOY);

        var pagina = service.listar(null, null, null, 1L, PageRequest.of(0, 20));

        assertThat(pagina.getContent()).extracting(BandejaItemResponse::itemId, BandejaItemResponse::todasVencidas)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(1L, true),
                        org.assertj.core.groups.Tuple.tuple(2L, false),
                        org.assertj.core.groups.Tuple.tuple(3L, false),
                        org.assertj.core.groups.Tuple.tuple(4L, false));
    }

    @Test
    void elDetalleTraeLasCotizacionesYElMismoCampo() {
        item(1L);
        cotizacion(1L, HOY.minusDays(1));
        item(2L);
        cotizacion(2L, HOY.minusDays(2));
        cotizacion(2L, HOY.plusDays(2));

        GestionItemDetalleResponse todasVencidas = service.obtener(1L);
        assertThat(todasVencidas.todasVencidas()).isTrue();
        assertThat(todasVencidas.cotizaciones()).hasSize(1);
        assertThat(todasVencidas.cotizaciones().get(0).vigente()).isFalse();
        assertThat(todasVencidas.cotizaciones().get(0).proveedorRazonSocial()).isEqualTo("Papelera SA");

        GestionItemDetalleResponse conVigente = service.obtener(2L);
        assertThat(conVigente.todasVencidas()).isFalse();
        assertThat(conVigente.cotizaciones()).extracting(c -> c.vigente()).containsExactlyInAnyOrder(true, false);
    }

    @Test
    void sinCotizacionesElDetalleNoMarcaNada() {
        item(1L);
        GestionItemDetalleResponse detalle = service.obtener(1L);
        assertThat(detalle.todasVencidas()).isFalse();
        assertThat(detalle.cotizaciones()).isEmpty();
    }

    @Test
    void proveedoresSugeridosSaleDeLaCategoriaDelItemYPuedeEstarVacio() {
        item(1L);
        when(catalogo.proveedoresActivosDe(5L)).thenReturn(List.of(new ProveedorResumen(9L, "Papelera SA", true)));

        assertThat(service.proveedoresSugeridos(1L)).singleElement().satisfies(p -> {
            assertThat(p.id()).isEqualTo(9L);
            assertThat(p.razonSocial()).isEqualTo("Papelera SA");
        });

        when(catalogo.proveedoresActivosDe(5L)).thenReturn(List.of());
        assertThat(service.proveedoresSugeridos(1L)).isEmpty();
    }

    @Test
    void proveedoresSugeridosDeUnItemInexistenteDa404() {
        assertThatThrownBy(() -> service.proveedoresSugeridos(99L)).isInstanceOf(NoEncontradoException.class);
    }
}
