package com.Sistem.Solicitude_Compra.catalogo.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Proveedor;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.CategoriaRepository;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.ProveedorRepository;
import com.Sistem.Solicitude_Compra.catalogo.web.ActualizarProveedorRequest;
import com.Sistem.Solicitude_Compra.catalogo.web.AltaProveedorRequest;
import com.Sistem.Solicitude_Compra.catalogo.web.ProveedorResponse;
import com.Sistem.Solicitude_Compra.compartido.errores.CuitDuplicadoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository proveedores;

    @Mock
    private CategoriaRepository categorias;

    private ProveedorService service;

    @BeforeEach
    void iniciar() {
        service = new ProveedorService(proveedores, categorias);
    }

    @Test
    void altaCreaElProveedorActivoConSusCategorias() {
        Categoria notebooks = categoria(10L, "Notebooks");
        when(categorias.findAllById(any())).thenReturn(List.of(notebooks));
        when(proveedores.existsByCuit("30-12345678-9")).thenReturn(false);
        when(proveedores.save(any(Proveedor.class))).thenAnswer(invocacion -> {
            Proveedor guardado = invocacion.getArgument(0);
            guardado.setId(1L);
            return guardado;
        });

        ProveedorResponse respuesta = service.alta(new AltaProveedorRequest(
                "Proveedor SA",
                "30-12345678-9",
                "1144445555",
                "compras@proveedor.com",
                "Ana",
                List.of(10L)));

        ArgumentCaptor<Proveedor> captor = ArgumentCaptor.forClass(Proveedor.class);
        verify(proveedores).save(captor.capture());
        Proveedor persistido = captor.getValue();
        assertThat(persistido.isActivo()).isTrue();
        assertThat(persistido.getRazonSocial()).isEqualTo("Proveedor SA");
        assertThat(persistido.getCategorias()).containsExactly(notebooks);

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.activo()).isTrue();
        assertThat(respuesta.categorias()).extracting("nombre").containsExactly("Notebooks");
    }

    @Test
    void altaConCuitDuplicadoLanza409() {
        when(proveedores.existsByCuit("30-12345678-9")).thenReturn(true);

        assertThatThrownBy(() -> service.alta(new AltaProveedorRequest(
                        "Otro SRL",
                        "30-12345678-9",
                        null,
                        null,
                        null,
                        List.of(10L))))
                .isInstanceOf(CuitDuplicadoException.class)
                .hasMessageContaining("30-12345678-9");

        verify(proveedores, never()).save(any());
        verify(categorias, never()).findAllById(any());
    }

    @Test
    void bajaDesactivaElProveedorSinBorrarlo() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(7L);
        proveedor.setRazonSocial("Proveedor SA");
        proveedor.setActivo(true);
        when(proveedores.findById(7L)).thenReturn(Optional.of(proveedor));
        when(proveedores.save(proveedor)).thenReturn(proveedor);

        ProveedorResponse respuesta = service.actualizar(7L, new ActualizarProveedorRequest(
                null, null, null, null, null, false, null));

        assertThat(respuesta.activo()).isFalse();
        assertThat(proveedor.isActivo()).isFalse();
        verify(proveedores).save(proveedor);
        verify(proveedores, never()).delete(any());
        verify(proveedores, never()).deleteById(any());
    }

    private Categoria categoria(Long id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        categoria.setTipo(TipoItem.PRODUCTO);
        return categoria;
    }
}
