package com.Sistem.Solicitude_Compra.catalogo.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.EstadoCategoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Proveedor;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.CategoriaRepository;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.ProveedorRepository;
import com.Sistem.Solicitude_Compra.catalogo.web.ActualizarCategoriaRequest;
import com.Sistem.Solicitude_Compra.catalogo.web.CategoriaResponse;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ErrorNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.eventos.CategoriaUnificada;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class CategoriaServiceTest {
    private CategoriaRepository categorias;
    private ProveedorRepository proveedores;
    private ApplicationEventPublisher eventos;
    private CategoriaService service;

    @BeforeEach
    void iniciar() {
        categorias = mock(CategoriaRepository.class);
        proveedores = mock(ProveedorRepository.class);
        eventos = mock(ApplicationEventPublisher.class);
        service = new CategoriaService(categorias, proveedores, eventos);
        when(categorias.saveAndFlush(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Categoria categoria(Long id, String nombre, EstadoCategoria estado) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre(nombre);
        c.setTipo(TipoItem.PRODUCTO);
        c.setEstado(estado);
        when(categorias.findById(id)).thenReturn(Optional.of(c));
        return c;
    }

    private static String codigoDe(Runnable accion) {
        try {
            accion.run();
        } catch (ErrorNegocioException e) {
            return e.getCodigo();
        }
        return null;
    }

    // ---- T06.6 ----

    @Test
    void confirmaUnaCategoriaNueva() {
        categoria(1L, "globos", EstadoCategoria.NUEVA);
        CategoriaResponse r = service.confirmar(1L, new ActualizarCategoriaRequest(EstadoCategoria.CONOCIDA, null));
        assertThat(r.estado()).isEqualTo(EstadoCategoria.CONOCIDA);
        assertThat(r.nombre()).isEqualTo("globos");
    }

    @Test
    void confirmarPuedeCorregirElNombre() {
        Categoria c = categoria(1L, "globos", EstadoCategoria.NUEVA);
        when(categorias.findByTipoAndNombreIgnoreCase(TipoItem.PRODUCTO, "Globos")).thenReturn(Optional.of(c));
        CategoriaResponse r = service.confirmar(1L, new ActualizarCategoriaRequest(EstadoCategoria.CONOCIDA, " Globos "));
        assertThat(r.nombre()).isEqualTo("Globos");
    }

    @Test
    void confirmarConUnNombreQueChocaConOtraDaDuplicado() {
        categoria(1L, "globos", EstadoCategoria.NUEVA);
        Categoria otra = categoria(2L, "Globos", EstadoCategoria.CONOCIDA);
        when(categorias.findByTipoAndNombreIgnoreCase(TipoItem.PRODUCTO, "Globos")).thenReturn(Optional.of(otra));
        assertThatThrownBy(() -> service.confirmar(1L, new ActualizarCategoriaRequest(EstadoCategoria.CONOCIDA, "Globos")))
                .isInstanceOfSatisfying(ConflictoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("DUPLICADO"));
        verify(categorias, never()).saveAndFlush(any());
    }

    @Test
    void conocidaYUnificadaDan422() {
        categoria(1L, "A", EstadoCategoria.CONOCIDA);
        categoria(2L, "B", EstadoCategoria.UNIFICADA);
        assertThat(codigoDe(() -> service.confirmar(1L, new ActualizarCategoriaRequest(EstadoCategoria.CONOCIDA, null))))
                .isEqualTo("TRANSICION_INVALIDA");
        assertThat(codigoDe(() -> service.confirmar(2L, new ActualizarCategoriaRequest(EstadoCategoria.CONOCIDA, null))))
                .isEqualTo("TRANSICION_INVALIDA");
    }

    @Test
    void soloSeAceptaElCambioAConocida() {
        categoria(1L, "A", EstadoCategoria.NUEVA);
        for (EstadoCategoria otro : List.of(EstadoCategoria.NUEVA, EstadoCategoria.UNIFICADA)) {
            assertThat(codigoDe(() -> service.confirmar(1L, new ActualizarCategoriaRequest(otro, null))))
                    .isEqualTo("TRANSICION_INVALIDA");
        }
    }

    @Test
    void categoriaInexistenteDa404() {
        when(categorias.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirmar(9L, new ActualizarCategoriaRequest(EstadoCategoria.CONOCIDA, null)))
                .isInstanceOf(NoEncontradoException.class);
    }

    // ---- T06.8 ----

    @Test
    void unificarMueveLosProveedoresSinDuplicarYPublicaElEvento() {
        Categoria origen = categoria(1L, "Globo", EstadoCategoria.NUEVA);
        Categoria destino = categoria(2L, "Globos", EstadoCategoria.CONOCIDA);
        Proveedor soloOrigen = new Proveedor();
        soloOrigen.getCategorias().add(origen);
        Proveedor ambas = new Proveedor();
        ambas.getCategorias().add(origen);
        ambas.getCategorias().add(destino);
        when(proveedores.findByCategorias_Id(1L)).thenReturn(List.of(soloOrigen, ambas));

        CategoriaResponse r = service.unificar(1L, 2L, 77L);

        assertThat(r.estado()).isEqualTo(EstadoCategoria.UNIFICADA);
        assertThat(origen.getUnificadaEn()).isSameAs(destino);
        assertThat(soloOrigen.getCategorias()).containsExactly(destino);
        assertThat(ambas.getCategorias()).containsExactly(destino);
        ArgumentCaptor<CategoriaUnificada> evento = ArgumentCaptor.forClass(CategoriaUnificada.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().origenId()).isEqualTo(1L);
        assertThat(evento.getValue().destinoId()).isEqualTo(2L);
        assertThat(evento.getValue().usuarioId()).isEqualTo(77L);
        assertThat(evento.getValue().eventoId()).isNotNull();
        assertThat(evento.getValue().ocurridoEn()).isNotNull();
    }

    @Test
    void unificarReapuntaLasQueYaApuntabanAlOrigen() {
        Categoria origen = categoria(1L, "B", EstadoCategoria.CONOCIDA);
        Categoria destino = categoria(2L, "C", EstadoCategoria.CONOCIDA);
        Categoria previa = categoria(3L, "A", EstadoCategoria.UNIFICADA);
        previa.setUnificadaEn(origen);
        when(categorias.findByUnificadaEn(origen)).thenReturn(List.of(previa));

        service.unificar(1L, 2L, 5L);

        assertThat(previa.getUnificadaEn()).isSameAs(destino);
    }

    @Test
    void unificarContraUnaUnificadaOConsigoMismaFallaSinPublicar() {
        categoria(1L, "A", EstadoCategoria.NUEVA);
        categoria(2L, "B", EstadoCategoria.UNIFICADA);
        assertThat(codigoDe(() -> service.unificar(1L, 2L, 5L))).isEqualTo("UNIFICACION_INVALIDA");
        assertThat(codigoDe(() -> service.unificar(1L, 1L, 5L))).isEqualTo("UNIFICACION_INVALIDA");
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void unificarConDestinoInexistenteDa404() {
        categoria(1L, "A", EstadoCategoria.NUEVA);
        when(categorias.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.unificar(1L, 9L, 5L)).isInstanceOf(NoEncontradoException.class);
    }
}
