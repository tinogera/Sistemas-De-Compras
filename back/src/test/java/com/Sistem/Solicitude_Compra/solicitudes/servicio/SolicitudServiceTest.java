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
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemCancelado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemEditado;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoItem;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Item;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.ItemRepository;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.SolicitudRepository;
import com.Sistem.Solicitude_Compra.solicitudes.web.CrearSolicitudRequest;
import com.Sistem.Solicitude_Compra.solicitudes.web.EditarItemRequest;
import com.Sistem.Solicitude_Compra.solicitudes.web.ItemRequest;
import com.Sistem.Solicitude_Compra.solicitudes.web.SolicitudDetalleResponse.ItemDetalle;
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
    @Mock ItemRepository itemRepositorio;
    @Mock AuthApi authApi;
    @Mock CatalogoApi catalogoApi;
    @Mock ApplicationEventPublisher eventos;

    SolicitudService service;

    @BeforeEach
    void preparar() {
        service = new SolicitudService(repositorio, itemRepositorio, authApi, catalogoApi, eventos);
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

    // ---- T08.1: editar un ítem PENDIENTE ----

    @Test
    void editaElItemActualizaLosCamposYPublicaItemEditado() {
        Item item = itemPendiente(4021L, 1587L, 7L, 3L);
        when(itemRepositorio.findByIdAndSolicitudId(4021L, 1587L)).thenReturn(Optional.of(item));
        when(itemRepositorio.saveAndFlush(item)).thenReturn(item);

        ItemDetalle editado = service.editarItem(1587L, 4021L, 7L,
                new EditarItemRequest(null, null, new BigDecimal("5"), "Resmas A4", new BigDecimal("2500"),
                        Moneda.USD, 3L));

        assertThat(editado.cantidad()).isEqualByComparingTo("5");
        assertThat(editado.detalle()).isEqualTo("Resmas A4");
        assertThat(editado.precioEstimado()).isEqualByComparingTo("2500");
        assertThat(editado.monedaEstimada()).isEqualTo(Moneda.USD);

        ArgumentCaptor<ItemEditado> captor = ArgumentCaptor.forClass(ItemEditado.class);
        verify(eventos).publishEvent(captor.capture());
        ItemEditado evento = captor.getValue();
        assertThat(evento.eventoId()).isNotNull();
        assertThat(evento.ocurridoEn()).isNotNull();
        assertThat(evento.itemId()).isEqualTo(4021L);
        assertThat(evento.solicitudId()).isEqualTo(1587L);
        assertThat(evento.usuarioId()).isEqualTo(7L);
        assertThat(evento.precioEstimado()).isEqualByComparingTo("2500");
        assertThat(evento.monedaEstimada()).isEqualTo(Moneda.USD);
    }

    @Test
    void conUnaCategoriaNuevaLaBuscaOCreaConElTipoDelItem() {
        Item item = itemPendiente(4021L, 1587L, 7L, 0L);
        when(itemRepositorio.findByIdAndSolicitudId(4021L, 1587L)).thenReturn(Optional.of(item));
        when(itemRepositorio.saveAndFlush(item)).thenReturn(item);
        when(catalogoApi.obtenerOCrearCategoria(TipoItem.PRODUCTO, "Globos")).thenReturn(99L);

        service.editarItem(1587L, 4021L, 7L,
                new EditarItemRequest(null, "  Globos ", null, null, null, null, 0L));

        assertThat(item.getCategoriaId()).isEqualTo(99L);
    }

    @Test
    void soloElDuenoDeLaSolicitudPuedeEditar() {
        Item item = itemPendiente(4021L, 1587L, 7L, 0L);
        when(itemRepositorio.findByIdAndSolicitudId(4021L, 1587L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.editarItem(1587L, 4021L, 8L, cambiarCantidad(0L)))
                .isInstanceOf(SinPermisoException.class);

        verify(itemRepositorio, never()).saveAndFlush(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void unItemQueYaNoEstaPendienteDa422ItemNoPendiente() {
        Item item = itemPendiente(4021L, 1587L, 7L, 0L);
        item.setEstado(EstadoItem.EN_COTIZACION);
        when(itemRepositorio.findByIdAndSolicitudId(4021L, 1587L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.editarItem(1587L, 4021L, 7L, cambiarCantidad(0L)))
                .isInstanceOfSatisfying(ReglaNegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo("ITEM_NO_PENDIENTE");
                    assertThat(e.getMessage()).isEqualTo("Este ítem ya está siendo cotizado");
                });

        verify(itemRepositorio, never()).saveAndFlush(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void conUnaVersionDistintaDa409YNoGuardaNiPublica() {
        Item item = itemPendiente(4021L, 1587L, 7L, 4L);
        when(itemRepositorio.findByIdAndSolicitudId(4021L, 1587L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.editarItem(1587L, 4021L, 7L, cambiarCantidad(3L)))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CONFLICTO_VERSION"));

        verify(itemRepositorio, never()).saveAndFlush(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void unItemInexistenteDa404() {
        when(itemRepositorio.findByIdAndSolicitudId(9L, 1587L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editarItem(1587L, 9L, 7L, cambiarCantidad(0L)))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void unaCategoriaInexistenteDa422YNoGuarda() {
        Item item = itemPendiente(4021L, 1587L, 7L, 0L);
        when(itemRepositorio.findByIdAndSolicitudId(4021L, 1587L)).thenReturn(Optional.of(item));
        when(catalogoApi.existeCategoria(500L)).thenReturn(false);

        assertThatThrownBy(() -> service.editarItem(1587L, 4021L, 7L,
                new EditarItemRequest(500L, null, null, null, null, null, 0L)))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CATEGORIA_INEXISTENTE"));

        verify(itemRepositorio, never()).saveAndFlush(any());
    }

    // ---- T08.2: cancelar un ítem PENDIENTE ----

    @Test
    void cancelarPasaElItemACanceladoPublicaItemCanceladoYNoTocaLaSolicitudSiQuedanOtros() {
        Item item = itemPendiente(4021L, 1587L, 7L, 2L);
        Item otro = new Item();
        otro.setId(4022L);
        item.getSolicitud().agregarItem(otro);
        when(itemRepositorio.findByIdAndSolicitudId(4021L, 1587L)).thenReturn(Optional.of(item));

        service.cancelarItem(1587L, 4021L, 7L, 2L);

        assertThat(item.getEstado()).isEqualTo(EstadoItem.CANCELADO);
        assertThat(item.getSolicitud().getEstado()).isEqualTo(EstadoSolicitud.EN_REVISION);
        verify(itemRepositorio).saveAndFlush(item);
        verify(repositorio, never()).save(any(Solicitud.class));

        ArgumentCaptor<ItemCancelado> captor = ArgumentCaptor.forClass(ItemCancelado.class);
        verify(eventos).publishEvent(captor.capture());
        assertThat(captor.getValue().itemId()).isEqualTo(4021L);
        assertThat(captor.getValue().solicitudId()).isEqualTo(1587L);
        assertThat(captor.getValue().usuarioId()).isEqualTo(7L);
        assertThat(captor.getValue().eventoId()).isNotNull();
    }

    @Test
    void siTodosLosItemsQuedanCanceladosLaSolicitudPasaACancelada() {
        Item item = itemPendiente(4021L, 1587L, 7L, 0L);
        Item otro = new Item();
        otro.setId(4022L);
        otro.setEstado(EstadoItem.CANCELADO);
        item.getSolicitud().agregarItem(otro);
        when(itemRepositorio.findByIdAndSolicitudId(4021L, 1587L)).thenReturn(Optional.of(item));

        service.cancelarItem(1587L, 4021L, 7L, 0L);

        assertThat(item.getSolicitud().getEstado()).isEqualTo(EstadoSolicitud.CANCELADA);
        verify(repositorio).save(item.getSolicitud());
    }

    @Test
    void siUnItemYaFueResueltoLaSolicitudNoPasaACancelada() {
        Item item = itemPendiente(4021L, 1587L, 7L, 0L);
        Item comprado = new Item();
        comprado.setId(4022L);
        comprado.setEstado(EstadoItem.COMPRADO);
        item.getSolicitud().agregarItem(comprado);
        when(itemRepositorio.findByIdAndSolicitudId(4021L, 1587L)).thenReturn(Optional.of(item));

        service.cancelarItem(1587L, 4021L, 7L, 0L);

        assertThat(item.getSolicitud().getEstado()).isEqualTo(EstadoSolicitud.EN_REVISION);
        verify(repositorio, never()).save(any(Solicitud.class));
    }

    @Test
    void cancelarElItemDeOtroDa403() {
        Item item = itemPendiente(4021L, 1587L, 7L, 0L);
        when(itemRepositorio.findByIdAndSolicitudId(4021L, 1587L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.cancelarItem(1587L, 4021L, 8L, 0L))
                .isInstanceOf(SinPermisoException.class);

        assertThat(item.getEstado()).isEqualTo(EstadoItem.PENDIENTE);
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void cancelarUnItemQueYaNoEstaPendienteDa422() {
        Item item = itemPendiente(4021L, 1587L, 7L, 0L);
        item.setEstado(EstadoItem.EN_COTIZACION);
        when(itemRepositorio.findByIdAndSolicitudId(4021L, 1587L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.cancelarItem(1587L, 4021L, 7L, 0L))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("ITEM_NO_PENDIENTE"));

        verify(itemRepositorio, never()).saveAndFlush(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void cancelarConUnaVersionDistintaDa409() {
        Item item = itemPendiente(4021L, 1587L, 7L, 4L);
        when(itemRepositorio.findByIdAndSolicitudId(4021L, 1587L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.cancelarItem(1587L, 4021L, 7L, 3L))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CONFLICTO_VERSION"));

        assertThat(item.getEstado()).isEqualTo(EstadoItem.PENDIENTE);
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void cancelarUnItemInexistenteDa404() {
        when(itemRepositorio.findByIdAndSolicitudId(9L, 1587L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelarItem(1587L, 9L, 7L, 0L))
                .isInstanceOf(NoEncontradoException.class);
    }

    private static EditarItemRequest cambiarCantidad(Long version) {
        return new EditarItemRequest(null, null, new BigDecimal("3"), null, null, null, version);
    }

    private static Item itemPendiente(Long itemId, Long solicitudId, Long solicitanteId, Long version) {
        Solicitud solicitud = new Solicitud();
        solicitud.setId(solicitudId);
        solicitud.setSolicitanteId(solicitanteId);
        Item item = new Item();
        item.setId(itemId);
        solicitud.agregarItem(item);
        item.setTipo(TipoItem.PRODUCTO);
        item.setCategoriaId(42L);
        item.setCantidad(new BigDecimal("1"));
        item.setDetalle("Resmas");
        item.setPrecioEstimado(new BigDecimal("1000"));
        item.setMonedaEstimada(Moneda.ARS);
        item.setVersion(version);
        return item;
    }

    private static CrearSolicitudRequest request(ItemRequest... items) {
        return new CrearSolicitudRequest(Urgencia.ALTA, LocalDate.now().plusDays(5), "obs", List.of(items));
    }

    private static ItemRequest item(Long categoriaId, String categoriaNueva, String detalle, String precio) {
        return new ItemRequest(TipoItem.PRODUCTO, categoriaId, categoriaNueva, new BigDecimal("1"), detalle,
                new BigDecimal(precio), Moneda.ARS);
    }
}
