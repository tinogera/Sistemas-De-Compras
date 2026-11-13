package com.Sistem.Solicitude_Compra.solicitudes.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.ClaveIdempotencia;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Item;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.ClaveIdempotenciaRepository;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.ItemRepository;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.SolicitudRepository;
import com.Sistem.Solicitude_Compra.solicitudes.web.CrearSolicitudRequest;
import com.Sistem.Solicitude_Compra.solicitudes.web.ItemRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** T08.7: Idempotency-Key al crear una solicitud. */
@ExtendWith(MockitoExtension.class)
class SolicitudServiceIdempotenciaTest {

    @Mock SolicitudRepository repositorio;
    @Mock ItemRepository itemRepositorio;
    @Mock AuthApi authApi;
    @Mock CatalogoApi catalogoApi;
    @Mock ApplicationEventPublisher eventos;
    @Mock ClaveIdempotenciaRepository claves;

    SolicitudService service;

    @BeforeEach
    void preparar() {
        service = new SolicitudService(repositorio, itemRepositorio, authApi, catalogoApi, eventos, claves);
    }

    @Test
    void sinClaveNoTocaElRepositorioDeClaves() {
        when(authApi.sectorDe(7L)).thenReturn(3L);
        when(catalogoApi.existeCategoria(42L)).thenReturn(true);
        when(repositorio.saveAndFlush(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        ResultadoCreacion resultado = service.crear(7L, request(), null);

        assertThat(resultado.creada()).isTrue();
        verify(claves, never()).findById(any());
        verify(claves, never()).saveAndFlush(any());
    }

    @Test
    void conUnaClaveNuevaLaGuardaConLaSolicitudAntesDePublicarElEvento() {
        UUID clave = UUID.randomUUID();
        when(authApi.sectorDe(7L)).thenReturn(3L);
        when(catalogoApi.existeCategoria(42L)).thenReturn(true);
        when(claves.findById(clave)).thenReturn(Optional.empty());
        when(repositorio.saveAndFlush(any(Solicitud.class))).thenAnswer(inv -> {
            Solicitud s = inv.getArgument(0);
            s.setId(1587L);
            return s;
        });

        ResultadoCreacion resultado = service.crear(7L, request(), clave);

        assertThat(resultado.creada()).isTrue();
        assertThat(resultado.solicitud().getId()).isEqualTo(1587L);
        ArgumentCaptor<ClaveIdempotencia> guardada = ArgumentCaptor.forClass(ClaveIdempotencia.class);
        InOrder orden = Mockito.inOrder(claves, eventos);
        orden.verify(claves).saveAndFlush(guardada.capture());
        orden.verify(eventos).publishEvent(any(SolicitudCreada.class));
        assertThat(guardada.getValue().getClave()).isEqualTo(clave);
        assertThat(guardada.getValue().getSolicitanteId()).isEqualTo(7L);
        assertThat(guardada.getValue().getSolicitudId()).isEqualTo(1587L);
    }

    @Test
    void conUnaClaveYaUsadaPorElMismoUsuarioDevuelveLaSolicitudDeAntesSinCrearNada() {
        UUID clave = UUID.randomUUID();
        Solicitud existente = new Solicitud();
        existente.setId(1587L);
        existente.setSolicitanteId(7L);
        existente.agregarItem(new Item());
        when(claves.findById(clave)).thenReturn(Optional.of(new ClaveIdempotencia(clave, 7L, 1587L)));
        when(repositorio.findConItemsById(1587L)).thenReturn(Optional.of(existente));

        ResultadoCreacion resultado = service.crear(7L, request(), clave);

        assertThat(resultado.creada()).isFalse();
        assertThat(resultado.solicitud()).isSameAs(existente);
        verify(repositorio, never()).saveAndFlush(any());
        verify(claves, never()).saveAndFlush(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void unaClaveYaUsadaPorOtroUsuarioDa422SinMostrarLaSolicitudAjena() {
        UUID clave = UUID.randomUUID();
        when(claves.findById(clave)).thenReturn(Optional.of(new ClaveIdempotencia(clave, 8L, 1587L)));

        assertThatThrownBy(() -> service.crear(7L, request(), clave))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CLAVE_IDEMPOTENCIA_INVALIDA"));
        verify(repositorio, never()).findConItemsById(any());
        verify(repositorio, never()).saveAndFlush(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    private static CrearSolicitudRequest request() {
        return new CrearSolicitudRequest(Urgencia.ALTA, LocalDate.now().plusDays(5), "obs",
                List.of(new ItemRequest(TipoItem.PRODUCTO, 42L, null, new BigDecimal("1"), "Pelotero",
                        new BigDecimal("180000"), Moneda.ARS)));
    }
}
