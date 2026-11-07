package com.Sistem.Solicitude_Compra.catalogo.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.EstadoCategoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Proveedor;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.CategoriaRepository;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.ProveedorRepository;
import com.Sistem.Solicitude_Compra.catalogo.web.ActualizarCategoriaRequest;
import com.Sistem.Solicitude_Compra.compartido.eventos.CategoriaUnificada;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, CategoriaService.class, CatalogoApi.class})
@RecordApplicationEvents
class CategoriaServiceIntegrationTest {
    @Autowired CategoriaService service;
    @Autowired CatalogoApi api;
    @Autowired CategoriaRepository categorias;
    @Autowired ProveedorRepository proveedores;
    @Autowired EntityManager em;
    @Autowired ApplicationEvents eventos;

    private Categoria categoria(String nombre, EstadoCategoria estado) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setTipo(TipoItem.PRODUCTO);
        c.setEstado(estado);
        return categorias.saveAndFlush(c);
    }

    private Proveedor proveedor(String razonSocial, Categoria... cats) {
        Proveedor p = new Proveedor();
        p.setRazonSocial(razonSocial);
        p.getCategorias().addAll(List.of(cats));
        return proveedores.saveAndFlush(p);
    }

    @Test
    void unificarMueveLosProveedoresEnLaBaseSinDuplicarFilas() {
        Categoria origen = categoria("Globo", EstadoCategoria.NUEVA);
        Categoria destino = categoria("Globos", EstadoCategoria.CONOCIDA);
        Proveedor soloOrigen = proveedor("Solo origen SA", origen);
        Proveedor ambas = proveedor("Ambas SA", origen, destino);

        service.unificar(origen.getId(), destino.getId(), 3L);
        em.flush();
        em.clear();

        assertThat(categorias.findById(origen.getId()).orElseThrow().getEstado()).isEqualTo(EstadoCategoria.UNIFICADA);
        assertThat(categorias.findById(origen.getId()).orElseThrow().getUnificadaEn().getId()).isEqualTo(destino.getId());
        assertThat(proveedores.findByCategorias_Id(origen.getId())).isEmpty();
        assertThat(proveedores.findByCategorias_Id(destino.getId()))
                .extracting(Proveedor::getId).containsExactlyInAnyOrder(soloOrigen.getId(), ambas.getId());
        Number filas = (Number) em.createNativeQuery(
                "select count(*) from catalogo.proveedor_categoria where proveedor_id = ?1")
                .setParameter(1, ambas.getId()).getSingleResult();
        assertThat(filas.intValue()).isEqualTo(1);
        assertThat(eventos.stream(CategoriaUnificada.class).toList()).hasSize(1);
        // T06.7: pedir los sugeridos de la unificada devuelve los de la destino
        assertThat(api.proveedoresActivosDe(origen.getId())).hasSize(2);
    }

    @Test
    void confirmarConElNombreDeOtraCategoriaDaDuplicadoYSinColisionRenombra() {
        Categoria nueva = categoria("globos", EstadoCategoria.NUEVA);
        categoria("Pelotas", EstadoCategoria.CONOCIDA);

        assertThatThrownBy(() -> service.confirmar(nueva.getId(),
                new ActualizarCategoriaRequest(EstadoCategoria.CONOCIDA, "pelotas")))
                .hasMessageContaining("Pelotas");

        var respuesta = service.confirmar(nueva.getId(),
                new ActualizarCategoriaRequest(EstadoCategoria.CONOCIDA, "Globos"));
        assertThat(respuesta.nombre()).isEqualTo("Globos");
        assertThat(respuesta.estado()).isEqualTo(EstadoCategoria.CONOCIDA);
    }

    @Test
    void elIndiceUnicoImpideDosCategoriasConElMismoNombreAunqueDifieraLaCapitalizacion() {
        categoria("Sillas", EstadoCategoria.CONOCIDA);
        assertThatThrownBy(() -> categoria("SILLAS", EstadoCategoria.NUEVA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
