package com.Sistem.Solicitude_Compra.catalogo.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.enums.EstadoCategoria;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CategoriaRepositoryTest {
    @Autowired CategoriaRepository categorias;

    @Test
    void buscaSinDistinguirMayusculasExcluyeUnificadasYLimitaEnOrden() {
        for (int i = 12; i >= 0; i--) {
            guardar("Globos %02d".formatted(i), TipoItem.PRODUCTO, EstadoCategoria.NUEVA);
        }
        guardar("Globos excluidos", TipoItem.PRODUCTO, EstadoCategoria.UNIFICADA);
        guardar("Globos servicio", TipoItem.SERVICIO, EstadoCategoria.NUEVA);
        categorias.flush();
        var resultado = categorias.findTop10ByTipoAndNombreContainingIgnoreCaseAndEstadoNot(
                TipoItem.PRODUCTO, "GLO", EstadoCategoria.UNIFICADA, Sort.by("nombre"));
        assertThat(resultado).hasSize(10);
        assertThat(resultado).extracting(Categoria::getNombre).containsExactly(
                "Globos 00", "Globos 01", "Globos 02", "Globos 03", "Globos 04",
                "Globos 05", "Globos 06", "Globos 07", "Globos 08", "Globos 09");
        assertThat(categorias.findTop10ByTipoAndNombreContainingIgnoreCaseAndEstadoNot(
                TipoItem.PRODUCTO, "excluidos", EstadoCategoria.UNIFICADA, Sort.by("nombre"))).isEmpty();
        assertThat(categorias.findTop10ByTipoAndNombreContainingIgnoreCaseAndEstadoNot(
                TipoItem.PRODUCTO, "servicio", EstadoCategoria.UNIFICADA, Sort.by("nombre"))).isEmpty();
    }

    private void guardar(String nombre, TipoItem tipo, EstadoCategoria estado) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setTipo(tipo);
        categoria.setEstado(estado);
        categorias.save(categoria);
    }
}