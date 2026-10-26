package com.Sistem.Solicitude_Compra.catalogo;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Proveedor;
import com.Sistem.Solicitude_Compra.catalogo.dominio.EstadoCategoria;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.CategoriaRepository;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.ProveedorRepository;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.compartido.eventos.CategoriaCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CatalogoApiTest {
    @Mock CategoriaRepository categorias;
    @Mock ProveedorRepository proveedores;
    @Mock ApplicationEventPublisher eventos;
    @InjectMocks CatalogoApi api;

    @Test
    void consultaExistenciaYNombre() {
        when(categorias.existsById(1L)).thenReturn(true);
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        assertThat(api.existeCategoria(1L)).isTrue();
        assertThat(api.existeCategoria(99L)).isFalse();
        assertThat(api.existeCategoria(null)).isFalse();
        assertThat(api.nombreCategoria(1L)).isEqualTo("Globos");
        assertThatThrownBy(() -> api.nombreCategoria(99L)).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void categoriaExistenteNoSeGuardaNiPublicaEvento() {
        when(categorias.findByTipoAndNombreIgnoreCase(TipoItem.PRODUCTO, "GLO"))
                .thenReturn(Optional.of(categoria(1L)));
        assertThat(api.obtenerOCrearCategoria(TipoItem.PRODUCTO, " GLO ")).isEqualTo(1L);
        verify(categorias, never()).save(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void resuelveLaCadenaDeCategoriasUnificadas() {
        Categoria origen = categoria(1L);
        Categoria intermedia = categoria(2L);
        Categoria destino = categoria(3L);
        origen.setEstado(EstadoCategoria.UNIFICADA);
        origen.setUnificadaEn(intermedia);
        intermedia.setEstado(EstadoCategoria.UNIFICADA);
        intermedia.setUnificadaEn(destino);
        when(categorias.findByTipoAndNombreIgnoreCase(TipoItem.PRODUCTO, "Globos"))
                .thenReturn(Optional.of(origen));
        assertThat(api.obtenerOCrearCategoria(TipoItem.PRODUCTO, "Globos")).isEqualTo(3L);
        verifyNoInteractions(eventos);
    }

    @Test
    void creaCategoriaNuevaYPublicaSusDatos() {
        when(categorias.save(any())).thenAnswer(invocacion -> {
            Categoria creada = invocacion.getArgument(0);
            assertThat(creada.getEstado()).isEqualTo(EstadoCategoria.NUEVA);
            creada.setId(8L);
            return creada;
        });
        assertThat(api.obtenerOCrearCategoria(TipoItem.SERVICIO, " Limpieza ")).isEqualTo(8L);
        ArgumentCaptor<CategoriaCreada> captor = ArgumentCaptor.forClass(CategoriaCreada.class);
        verify(eventos).publishEvent(captor.capture());
        CategoriaCreada evento = captor.getValue();
        assertThat(evento.categoriaId()).isEqualTo(8L);
        assertThat(evento.nombre()).isEqualTo("Limpieza");
        assertThat(evento.tipo()).isEqualTo(TipoItem.SERVICIO);
        assertThat(evento.eventoId()).isNotNull();
        assertThat(evento.ocurridoEn()).isNotNull();
    }

    @Test
    void rechazaDatosInvalidosAntesDeConsultar() {
        assertThatThrownBy(() -> api.obtenerOCrearCategoria(null, "Globos"))
                .isInstanceOf(ParametroInvalidoException.class);
        for (String nombre : new String[]{null, "", "   ", "a".repeat(121)}) {
            assertThatThrownBy(() -> api.obtenerOCrearCategoria(TipoItem.PRODUCTO, nombre))
                    .isInstanceOf(ParametroInvalidoException.class);
        }
        verifyNoInteractions(categorias, eventos);
    }

    @Test
    void devuelveResumenDeProveedorAunqueEsteInactivo() {
        Proveedor proveedor = proveedor(false);
        when(proveedores.findById(4L)).thenReturn(Optional.of(proveedor));
        assertThat(api.obtenerProveedor(4L)).isEqualTo(new ProveedorResumen(4L, "Proveedor SA", false));
        assertThatThrownBy(() -> api.obtenerProveedor(99L)).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void proveedoresSeBuscanPorLaCategoriaDestino() {
        Categoria origen = categoria(1L);
        origen.setEstado(EstadoCategoria.UNIFICADA);
        origen.setUnificadaEn(categoria(2L));
        when(categorias.findById(1L)).thenReturn(Optional.of(origen));
        when(proveedores.findByActivoTrueAndCategorias_Id(2L)).thenReturn(List.of(proveedor(true)));
        assertThat(api.proveedoresActivosDe(1L))
                .containsExactly(new ProveedorResumen(4L, "Proveedor SA", true));
        verify(proveedores, never()).findByActivoTrueAndCategorias_Id(1L);
    }

    @Test
    void categoriaSinProveedoresDevuelveListaVacia() {
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        assertThat(api.proveedoresActivosDe(1L)).isEmpty();
        assertThatThrownBy(() -> api.proveedoresActivosDe(99L)).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void unaUnificacionCiclicaNoGeneraUnBucleInfinito() {
        Categoria origen = categoria(1L);
        origen.setEstado(EstadoCategoria.UNIFICADA);
        origen.setUnificadaEn(origen);
        when(categorias.findByTipoAndNombreIgnoreCase(TipoItem.PRODUCTO, "Globos"))
                .thenReturn(Optional.of(origen));
        assertThatThrownBy(() -> api.obtenerOCrearCategoria(TipoItem.PRODUCTO, "Globos"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(eventos);
    }

    private Categoria categoria(Long id) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre("Globos");
        categoria.setTipo(TipoItem.PRODUCTO);
        return categoria;
    }

    private Proveedor proveedor(boolean activo) {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(4L);
        proveedor.setRazonSocial("Proveedor SA");
        proveedor.setActivo(activo);
        return proveedor;
    }
}