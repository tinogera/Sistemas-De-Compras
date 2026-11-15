package com.Sistem.Solicitude_Compra.solicitudes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.solicitudes.servicio.SolicitudService;
import com.Sistem.Solicitude_Compra.solicitudes.web.CrearSolicitudRequest;
import com.Sistem.Solicitude_Compra.solicitudes.web.ItemRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class SolicitudesModuloTest {

    @Autowired SolicitudService service;
    @MockitoBean AuthApi authApi;
    @MockitoBean CatalogoApi catalogoApi;
    @MockitoBean UsuarioActual usuarioActual;

    @Test
    void crearUnaSolicitudPublicaSolicitudCreadaConLosDatosCorrectos(Scenario scenario) {
        when(authApi.sectorDe(1L)).thenReturn(1L);
        when(catalogoApi.existeCategoria(5L)).thenReturn(true);
        var request = new CrearSolicitudRequest(Urgencia.ALTA, LocalDate.now().plusDays(4), "obs",
                List.of(new ItemRequest(TipoItem.SERVICIO, 5L, null, new BigDecimal("3"), "Pelotero",
                        new BigDecimal("180000"), Moneda.ARS)));

        scenario.stimulate(() -> service.crear(1L, request))
                .andWaitForEventOfType(SolicitudCreada.class)
                .toArriveAndVerify(evento -> {
                    assertThat(evento.solicitanteId()).isEqualTo(1L);
                    assertThat(evento.sectorId()).isEqualTo(1L);
                    assertThat(evento.urgencia()).isEqualTo(Urgencia.ALTA);
                    assertThat(evento.items()).hasSize(1);
                    var item = evento.items().get(0);
                    assertThat(item.itemId()).isNotNull();
                    assertThat(item.categoriaId()).isEqualTo(5L);
                    assertThat(item.precioEstimado()).isEqualByComparingTo("180000");
                    assertThat(item.monedaEstimada()).isEqualTo(Moneda.ARS);
                });
    }
}
