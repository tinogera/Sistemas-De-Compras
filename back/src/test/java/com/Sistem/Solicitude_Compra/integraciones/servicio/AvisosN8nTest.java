package com.Sistem.Solicitude_Compra.integraciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.aprobaciones.AprobacionesApi;
import com.Sistem.Solicitude_Compra.compartido.eventos.PasoAprobacionActivado;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import java.util.Optional;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.eventos.EncargadoDesasignado;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudLista;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvisosN8nTest {

    @Mock ClienteN8n n8n;
    @Mock AuthApi auth;
    @Mock CatalogoApi catalogo;
    @Mock AprobacionesApi aprobaciones;

    AvisosN8n avisos() {
        return new AvisosN8n(n8n, auth, catalogo, aprobaciones);
    }

    private static UsuarioResumen usuario(long id, String nombre, String email, Rol rol) {
        return new UsuarioResumen(id, nombre, email, rol, 2L, true);
    }

    @Test
    void solicitudCreadaAvisaALosEncargados() {
        when(auth.obtenerUsuario(5L)).thenReturn(usuario(5L, "Laura", "laura@x.com", Rol.SOLICITANTE));
        when(auth.nombreSector(2L)).thenReturn("Eventos");
        when(auth.emailsEncargadosActivos()).thenReturn(List.of("enc@x.com"));
        var item = new SolicitudCreada.ItemCreado(1L, TipoItem.PRODUCTO, 3L, BigDecimal.ONE, "Pelotero",
                BigDecimal.TEN, Moneda.ARS);

        avisos().alCrearseLaSolicitud(new SolicitudCreada(UUID.randomUUID(), Instant.now(), 1587L, 5L, 2L,
                Urgencia.ALTA, LocalDate.of(2026, 12, 1), List.of(item, item)));

        ArgumentCaptor<Object> datos = ArgumentCaptor.forClass(Object.class);
        verify(n8n).enviar(eq("solicitud-creada"), datos.capture());
        assertEquals(new AvisosN8n.SolicitudNueva(1587L, "Laura", "Eventos", "ALTA", LocalDate.of(2026, 12, 1), 2,
                List.of("enc@x.com")), datos.getValue());
    }

    @Test
    void solicitudListaNoIncluyeLosItemsCancelados() {
        when(auth.obtenerUsuario(5L)).thenReturn(usuario(5L, "Laura", "laura@x.com", Rol.SOLICITANTE));
        when(catalogo.nombreCategoria(3L)).thenReturn("Juegos");
        var comprado = new SolicitudLista.ResultadoItem(1L, 3L, "Pelotero", "COMPRADO", "Don José", null, null);
        var cancelado = new SolicitudLista.ResultadoItem(2L, 3L, "Globos", "CANCELADO", null, null, null);

        avisos().alQuedarListaLaSolicitud(new SolicitudLista(UUID.randomUUID(), Instant.now(), 1587L, 5L,
                List.of(comprado, cancelado)));

        ArgumentCaptor<Object> datos = ArgumentCaptor.forClass(Object.class);
        verify(n8n).enviar(eq("solicitud-lista"), datos.capture());
        assertEquals(new AvisosN8n.ResultadoSolicitud(1587L, "laura@x.com", "Laura",
                List.of(new AvisosN8n.ItemResultado("Juegos", "Pelotero", "COMPRADO", "Don José", null, null))),
                datos.getValue());
    }

    @Test
    void encargadoDesasignadoAvisaALosDemasEncargados() {
        when(auth.obtenerUsuario(9L)).thenReturn(usuario(9L, "Pedro", "pedro@x.com", Rol.SOLICITANTE));
        when(auth.emailsEncargadosActivos()).thenReturn(List.of("pedro@x.com", "ana@x.com"));

        avisos().alDesasignarseUnEncargado(new EncargadoDesasignado(UUID.randomUUID(), Instant.now(), 9L, 1L));

        ArgumentCaptor<Object> datos = ArgumentCaptor.forClass(Object.class);
        verify(n8n).enviar(eq("encargado-desasignado"), datos.capture());
        assertEquals(new AvisosN8n.EncargadoQuitado("Pedro", List.of("ana@x.com")), datos.getValue());
    }

    private static PasoAprobacionActivado paso(String tipo, Rol rol, Long sector) {
        return new PasoAprobacionActivado(UUID.randomUUID(), Instant.now(), 7L, 1587L, 70L, tipo, rol, sector,
                "Supervisor", new BigDecimal("600000"), Moneda.ARS);
    }

    @Test
    void pasoActivadoAvisaALosAprobadoresMenosAlSolicitante() {
        when(aprobaciones.datosParaAviso(7L)).thenReturn(
                Optional.of(new AprobacionesApi.DatosParaAviso(5L, 2L, "Pelotero")));
        when(auth.obtenerUsuario(5L)).thenReturn(usuario(5L, "Laura", "laura@x.com", Rol.SUPERVISOR));
        when(auth.nombreSector(2L)).thenReturn("Eventos");
        when(auth.emailsActivosConRol(Rol.SUPERVISOR)).thenReturn(List.of("laura@x.com", "sup@x.com"));

        avisos().alActivarseUnPaso(paso("ROL", Rol.SUPERVISOR, null));

        ArgumentCaptor<Object> datos = ArgumentCaptor.forClass(Object.class);
        verify(n8n).enviar(eq("aprobacion-pendiente"), datos.capture());
        assertEquals(new AvisosN8n.AprobacionPendiente(7L, 1587L, "Pelotero", "Laura", "Eventos", "Supervisor",
                new BigDecimal("600000"), Moneda.ARS, List.of("sup@x.com")), datos.getValue());
    }

    @Test
    void pasoDeSectorAvisaALosUsuariosDelSector() {
        when(aprobaciones.datosParaAviso(7L)).thenReturn(
                Optional.of(new AprobacionesApi.DatosParaAviso(5L, 2L, "Pelotero")));
        when(auth.obtenerUsuario(5L)).thenReturn(usuario(5L, "Laura", "laura@x.com", Rol.SOLICITANTE));
        when(auth.nombreSector(2L)).thenReturn("Eventos");
        when(auth.emailsActivosDelSector(9L)).thenReturn(List.of("gg@x.com"));

        avisos().alActivarseUnPaso(paso("SECTOR", null, 9L));

        verify(n8n).enviar(eq("aprobacion-pendiente"), any());
    }

    @Test
    void sinDestinatariosNoSeEnviaNada() {
        when(aprobaciones.datosParaAviso(7L)).thenReturn(
                Optional.of(new AprobacionesApi.DatosParaAviso(5L, 2L, "Pelotero")));
        when(auth.obtenerUsuario(5L)).thenReturn(usuario(5L, "Laura", "laura@x.com", Rol.SUPERVISOR));
        when(auth.emailsActivosConRol(Rol.SUPERVISOR)).thenReturn(List.of("laura@x.com"));

        avisos().alActivarseUnPaso(paso("ROL", Rol.SUPERVISOR, null));

        verify(n8n, never()).enviar(anyString(), any());
    }
}
