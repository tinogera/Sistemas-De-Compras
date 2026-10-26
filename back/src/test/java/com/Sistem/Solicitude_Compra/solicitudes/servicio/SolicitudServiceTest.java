package com.Sistem.Solicitude_Compra.solicitudes.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.SolicitudRepository;
import com.Sistem.Solicitude_Compra.solicitudes.web.CrearSolicitudRequest;
import com.Sistem.Solicitude_Compra.solicitudes.web.ItemRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    @Mock SolicitudRepository repositorio;
    @Mock AuthApi authApi;
    @Mock CatalogoApi catalogoApi;
    @Mock ApplicationEventPublisher eventos;

    SolicitudService service;

    @BeforeEach
    void preparar() {
        service = new SolicitudService(repositorio, authApi, catalogoApi, eventos);
    }

    @Test
    void creaLaSolicitudCopiaElSectorYPublicaSolicitudCreada() {
        when(authApi.sectorDe(7L)).thenReturn(3L);
        when(catalogoApi.existeCategoria(42L)).thenReturn(true);
        when(catalogoApi.obtenerOCrearCategoria(TipoItem.PRODUCTO, "Globos")).thenReturn(99L);
        when(repositorio.saveAndFlush(any(Solicitud.class))).thenAnswer(inv -> {
            Solicitud s = inv.getArgument(0);
            s.setId(1587L);
            long id = 4021;
            for (var item : s.getItems()) {
                item.setId(id++);
            }
            return s;
        });

        Solicitud creada = service.crear(7L, request(
                item(42L, null, "Pelotero", "180000"),
                item(null, "Globos", null, "45000")));

        assertThat(creada.getSectorId()).isEqualTo(3L);
        assertThat(creada.getSolicitanteId()).isEqualTo(7L);
        assertThat(creada.getItems()).hasSize(2);
        assertThat(creada.getItems().get(1).getCategoriaId()).isEqualTo(99L);
        // Sin detalle se usa el nombre de la categoría nueva.
        assertThat(creada.getItems().get(1).getDetalle()).isEqualTo("Globos");

        ArgumentCaptor<SolicitudCreada> captor = ArgumentCaptor.forClass(SolicitudCreada.class);
        verify(eventos).publishEvent(captor.capture());
        SolicitudCreada evento = captor.getValue();
        assertThat(evento.eventoId()).isNotNull();
        assertThat(evento.ocurridoEn()).isNotNull();
        assertThat(evento.solicitudId()).isEqualTo(1587L);
        assertThat(evento.sectorId()).isEqualTo(3L);
        assertThat(evento.items()).hasSize(2);
        assertThat(evento.items().get(0).itemId()).isEqualTo(4021L);
        assertThat(evento.items().get(0).precioEstimado()).isEqualByComparingTo("180000");
        assertThat(evento.items().get(0).monedaEstimada()).isEqualTo(Moneda.ARS);
    }

    @Test
    void conUnaCategoriaInexistenteDa422YNoGuardaNiPublica() {
        when(authApi.sectorDe(7L)).thenReturn(3L);
        when(catalogoApi.existeCategoria(500L)).thenReturn(false);

        assertThatThrownBy(() -> service.crear(7L, request(item(500L, null, "x", "10"))))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CATEGORIA_INEXISTENTE"));

        verify(repositorio, never()).saveAndFlush(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void unSolicitanteNoPuedeVerLaSolicitudDeOtro() {
        Solicitud ajena = new Solicitud();
        ajena.setSolicitanteId(8L);
        when(repositorio.findConItemsById(1L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.obtener(1L, 7L, Rol.SOLICITANTE))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void unaSolicitudInexistenteDa404() {
        when(repositorio.findConItemsById(eq(9L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(9L, 7L, Rol.ENCARGADO))
                .isInstanceOf(NoEncontradoException.class);
    }

    private static CrearSolicitudRequest request(ItemRequest... items) {
        return new CrearSolicitudRequest(Urgencia.ALTA, LocalDate.now().plusDays(5), "obs", List.of(items));
    }

    private static ItemRequest item(Long categoriaId, String categoriaNueva, String detalle, String precio) {
        return new ItemRequest(TipoItem.PRODUCTO, categoriaId, categoriaNueva, new BigDecimal("1"), detalle,
                new BigDecimal(precio), Moneda.ARS);
    }
}
