package com.Sistem.Solicitude_Compra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Proveedor;
import com.Sistem.Solicitude_Compra.catalogo.dominio.EstadoCategoria;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.CategoriaRepository;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.ProveedorRepository;
import com.Sistem.Solicitude_Compra.compartido.eventos.CategoriaCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, CatalogoApi.class})
@RecordApplicationEvents
class CatalogoApiIntegrationTest {
    @Autowired CatalogoApi api;
    @Autowired CategoriaRepository categorias;
    @Autowired ProveedorRepository proveedores;
    @Autowired ApplicationEvents eventos;

    @Test
    void persisteUnaSolaCategoriaYPublicaUnSoloEvento() {
        Long id = api.obtenerOCrearCategoria(TipoItem.PRODUCTO, " Globos ");
        categorias.flush();
        assertThat(api.obtenerOCrearCategoria(TipoItem.PRODUCTO, "gLoBoS")).isEqualTo(id);
        assertThat(api.existeCategoria(id)).isTrue();
        assertThat(api.nombreCategoria(id)).isEqualTo("Globos");
        assertThat(categorias.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoCategoria.NUEVA);
        assertThat(eventos.stream(CategoriaCreada.class).toList()).hasSize(1);
        Long servicio = api.obtenerOCrearCategoria(TipoItem.SERVICIO, "Globos");
        assertThat(servicio).isNotEqualTo(id);
    }

    @Test
    void devuelveSoloProveedoresActivosDeLaCategoriaDestino() {
        Long destinoId = api.obtenerOCrearCategoria(TipoItem.PRODUCTO, "Globos");
        Categoria destino = categorias.findById(destinoId).orElseThrow();
        Categoria origen = new Categoria();
        origen.setNombre("Globos antiguos");
        origen.setTipo(TipoItem.PRODUCTO);
        origen.setEstado(EstadoCategoria.UNIFICADA);
        origen.setUnificadaEn(destino);
        categorias.saveAndFlush(origen);
        Proveedor activo = guardarProveedor("Activo", true, destino);
        guardarProveedor("Inactivo", false, destino);
        Categoria otra = categorias.findById(api.obtenerOCrearCategoria(TipoItem.PRODUCTO, "Otra")).orElseThrow();
        guardarProveedor("Otra categoria", true, otra);
        proveedores.flush();

        assertThat(api.obtenerOCrearCategoria(TipoItem.PRODUCTO, "GLOBOS ANTIGUOS")).isEqualTo(destinoId);
        assertThat(api.proveedoresActivosDe(origen.getId()))
                .containsExactly(new ProveedorResumen(activo.getId(), "Activo", true));
        assertThat(api.obtenerProveedor(activo.getId()))
                .isEqualTo(new ProveedorResumen(activo.getId(), "Activo", true));
    }

    private Proveedor guardarProveedor(String nombre, boolean activo, Categoria categoria) {
        Proveedor proveedor = new Proveedor();
        proveedor.setRazonSocial(nombre);
        proveedor.setActivo(activo);
        proveedor.setCategorias(Set.of(categoria));
        return proveedores.save(proveedor);
    }
}