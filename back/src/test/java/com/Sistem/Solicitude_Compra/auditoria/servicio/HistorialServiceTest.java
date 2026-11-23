package com.Sistem.Solicitude_Compra.auditoria.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EntradaHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.ItemSolicitud;
import com.Sistem.Solicitude_Compra.auditoria.repositorio.HistorialRepository;
import com.Sistem.Solicitude_Compra.auditoria.repositorio.ItemSolicitudRepository;
import com.Sistem.Solicitude_Compra.auditoria.web.HistorialResponse;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistorialServiceTest {

    private static final Long DUENO = 5L;

    HistorialRepository historial = mock(HistorialRepository.class);
    ItemSolicitudRepository items = mock(ItemSolicitudRepository.class);
    AuthApi authApi = mock(AuthApi.class);
    HistorialService service = new HistorialService(historial, items, authApi);

    private EntradaHistorial entrada(EntidadHistorial entidad, Long id, String hora, Long usuario) {
        return EntradaHistorial.de(entidad, id, AccionHistorial.CAMBIO_ESTADO, "A", "B", usuario, null,
                Instant.parse("2026-11-01T" + hora + "Z"));
    }

    private void solicitudConItems() {
        when(items.findBySolicitudId(1L)).thenReturn(List.of(new ItemSolicitud(11L, 1L, DUENO),
                new ItemSolicitud(12L, 1L, DUENO)));
        when(historial.findByEntidadAndEntidadIdOrderByOcurridoEnAsc(EntidadHistorial.SOLICITUD, 1L))
                .thenReturn(List.of(entrada(EntidadHistorial.SOLICITUD, 1L, "10:00:00", DUENO)));
        when(authApi.obtenerUsuario(DUENO)).thenReturn(
                new UsuarioResumen(DUENO, "Ana", "a@test.com", Rol.SOLICITANTE, 1L, true));
    }

    @Test
    void elDuenoVeElHistorialDeSuSolicitud() {
        solicitudConItems();

        List<HistorialResponse> lista = service.consultar(EntidadHistorial.SOLICITUD, 1L, false, DUENO,
                Rol.SOLICITANTE);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).usuarioNombre()).isEqualTo("Ana");
        assertThat(lista.get(0).estadoNuevo()).isEqualTo("B");
    }

    @Test
    void otroSolicitanteRecibe403() {
        solicitudConItems();

        assertThatThrownBy(() -> service.consultar(EntidadHistorial.SOLICITUD, 1L, false, 99L, Rol.SOLICITANTE))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void unSolicitanteNoPuedePedirOtrasEntidades() {
        assertThatThrownBy(() -> service.consultar(EntidadHistorial.ORDEN, 1L, false, DUENO, Rol.SOLICITANTE))
                .isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> service.consultar(EntidadHistorial.ITEM, 11L, false, DUENO, Rol.SOLICITANTE))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void unaSolicitudDesconocidaParaUnSolicitanteEs403() {
        assertThatThrownBy(() -> service.consultar(EntidadHistorial.SOLICITUD, 77L, false, DUENO, Rol.SOLICITANTE))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void elEncargadoYElAdminVenCualquierHistorial() {
        when(historial.findByEntidadAndEntidadIdOrderByOcurridoEnAsc(EntidadHistorial.ORDEN, 3L))
                .thenReturn(List.of(entrada(EntidadHistorial.ORDEN, 3L, "10:00:00", null)));

        assertThat(service.consultar(EntidadHistorial.ORDEN, 3L, false, 1L, Rol.ENCARGADO)).hasSize(1);
        assertThat(service.consultar(EntidadHistorial.ORDEN, 3L, false, 2L, Rol.ADMIN)).hasSize(1);
    }

    @Test
    void sinEntradasDevuelveListaVacia() {
        assertThat(service.consultar(EntidadHistorial.COTIZACION, 8L, false, 1L, Rol.ENCARGADO)).isEmpty();
    }

    @Test
    void unCambioSinUsuarioSeMuestraComoSistema() {
        when(historial.findByEntidadAndEntidadIdOrderByOcurridoEnAsc(EntidadHistorial.ORDEN, 3L))
                .thenReturn(List.of(entrada(EntidadHistorial.ORDEN, 3L, "10:00:00", null)));

        var lista = service.consultar(EntidadHistorial.ORDEN, 3L, false, 1L, Rol.ENCARGADO);

        assertThat(lista.get(0).usuarioNombre()).isEqualTo("Sistema");
    }

    @Test
    void conIncluirItemsMezclaLasEntradasDeLosItemsOrdenadasPorFecha() {
        solicitudConItems();
        when(historial.findByEntidadAndEntidadIdInOrderByOcurridoEnAsc(any(), any())).thenReturn(List.of(
                entrada(EntidadHistorial.ITEM, 11L, "09:00:00", DUENO),
                entrada(EntidadHistorial.ITEM, 12L, "11:00:00", DUENO)));

        var lista = service.consultar(EntidadHistorial.SOLICITUD, 1L, true, DUENO, Rol.SOLICITANTE);

        assertThat(lista).extracting(HistorialResponse::entidad)
                .containsExactly(EntidadHistorial.ITEM, EntidadHistorial.SOLICITUD, EntidadHistorial.ITEM);
    }

    @Test
    void sinIncluirItemsNoSeBuscanLasEntradasDeLosItems() {
        solicitudConItems();

        var lista = service.consultar(EntidadHistorial.SOLICITUD, 1L, false, DUENO, Rol.SOLICITANTE);

        assertThat(lista).extracting(HistorialResponse::entidad).containsExactly(EntidadHistorial.SOLICITUD);
        org.mockito.Mockito.verify(historial, org.mockito.Mockito.never())
                .findByEntidadAndEntidadIdInOrderByOcurridoEnAsc(any(), any());
    }

    @Test
    void elNombreDelUsuarioSePideUnaSolaVez() {
        solicitudConItems();
        when(historial.findByEntidadAndEntidadIdOrderByOcurridoEnAsc(EntidadHistorial.SOLICITUD, 1L)).thenReturn(
                List.of(entrada(EntidadHistorial.SOLICITUD, 1L, "10:00:00", DUENO),
                        entrada(EntidadHistorial.SOLICITUD, 1L, "11:00:00", DUENO)));

        service.consultar(EntidadHistorial.SOLICITUD, 1L, false, DUENO, Rol.SOLICITANTE);

        org.mockito.Mockito.verify(authApi, org.mockito.Mockito.times(1)).obtenerUsuario(anyLong());
    }
}
