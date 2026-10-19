package com.Sistem.Solicitude_Compra.catalogo.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.catalogo.dominio.enums.EstadoCategoria;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CatalogoDominioTest {

    @Autowired
    EntityManager em;

    @Test
    void laCategoriaNuevaQuedaEnEstadoNueva() {
        Categoria categoria = categoria("Notebooks", TipoItem.PRODUCTO);

        em.persist(categoria);
        em.flush();
        em.clear();

        Categoria guardada = em.find(Categoria.class, categoria.getId());
        assertThat(guardada.getEstado()).isEqualTo(EstadoCategoria.NUEVA);
        assertThat(guardada.getUnificadaEn()).isNull();
    }

    @Test
    void noPermiteDosCategoriasConElMismoNombreYTipoSinImportarMayusculas() {
        em.persist(categoria("Notebooks", TipoItem.PRODUCTO));
        em.flush();

        assertThatThrownBy(() -> {
            em.persist(categoria("NOTEBOOKS", TipoItem.PRODUCTO));
            em.flush();
        }).isInstanceOf(PersistenceException.class);
    }

    @Test
    void permiteElMismoNombreEnOtroTipo() {
        em.persist(categoria("Limpieza", TipoItem.PRODUCTO));
        em.flush();

        assertThatCode(() -> {
            em.persist(categoria("Limpieza", TipoItem.SERVICIO));
            em.flush();
        }).doesNotThrowAnyException();
    }

    @Test
    void laCategoriaUnificadaApuntaALaCategoriaDestino() {
        Categoria destino = categoria("Notebooks", TipoItem.PRODUCTO);
        em.persist(destino);
        Categoria vieja = categoria("Laptops", TipoItem.PRODUCTO);
        vieja.setEstado(EstadoCategoria.UNIFICADA);
        vieja.setUnificadaEn(destino);
        em.persist(vieja);
        em.flush();
        em.clear();

        Categoria guardada = em.find(Categoria.class, vieja.getId());
        assertThat(guardada.getUnificadaEn().getId()).isEqualTo(destino.getId());
    }

    @Test
    void elProveedorQuedaActivoPorDefectoYGuardaSusCategorias() {
        Categoria categoria = categoria("Notebooks", TipoItem.PRODUCTO);
        em.persist(categoria);
        Proveedor proveedor = proveedor("Proveedor SA", "30-12345678-9");
        proveedor.getCategorias().add(categoria);
        em.persist(proveedor);
        em.flush();
        em.clear();

        Proveedor guardado = em.find(Proveedor.class, proveedor.getId());
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getCategorias()).extracting(Categoria::getNombre).containsExactly("Notebooks");
    }

    @Test
    void noPermiteDosProveedoresConElMismoCuit() {
        em.persist(proveedor("Proveedor SA", "30-12345678-9"));
        em.flush();

        assertThatThrownBy(() -> {
            em.persist(proveedor("Otro SRL", "30-12345678-9"));
            em.flush();
        }).isInstanceOf(PersistenceException.class);
    }

    @Test
    void permiteProveedoresSinCuit() {
        em.persist(proveedor("Uno SA", null));
        em.persist(proveedor("Dos SA", null));

        assertThatCode(() -> em.flush()).doesNotThrowAnyException();
    }

    @Test
    void rechazaUnCuitConFormatoInvalido() {
        assertThatThrownBy(() -> {
            em.persist(proveedor("Proveedor SA", "30123456789"));
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void rechazaUnEmailInvalido() {
        Proveedor proveedor = proveedor("Proveedor SA", null);
        proveedor.setEmail("esto-no-es-un-mail");

        assertThatThrownBy(() -> {
            em.persist(proveedor);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    private Categoria categoria(String nombre, TipoItem tipo) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setTipo(tipo);
        return categoria;
    }

    private Proveedor proveedor(String razonSocial, String cuit) {
        Proveedor proveedor = new Proveedor();
        proveedor.setRazonSocial(razonSocial);
        proveedor.setCuit(cuit);
        return proveedor;
    }
}
