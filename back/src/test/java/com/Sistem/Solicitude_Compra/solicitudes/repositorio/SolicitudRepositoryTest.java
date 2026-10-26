package com.Sistem.Solicitude_Compra.solicitudes.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Item;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class SolicitudRepositoryTest {

    @Autowired SolicitudRepository solicitudes;
    @Autowired ItemRepository items;
    @Autowired EntityManager em;

    @Test
    void findConItemsByIdTraeLaSolicitudConSusDosItems() {
        Long id = solicitudes.saveAndFlush(solicitud(1L, 2)).getId();
        em.clear();

        Solicitud encontrada = solicitudes.findConItemsById(id).orElseThrow();

        assertThat(encontrada.getItems()).hasSize(2);
        assertThat(encontrada.getEstado()).isEqualTo(EstadoSolicitud.EN_REVISION);
        assertThat(encontrada.getVersion()).isNotNull();
    }

    @Test
    void listaSoloLasDeUnSolicitanteConFiltroDeEstado() {
        solicitudes.saveAndFlush(solicitud(10L, 1));
        solicitudes.saveAndFlush(solicitud(10L, 1));
        solicitudes.saveAndFlush(solicitud(11L, 1));
        var orden = PageRequest.of(0, 10, Sort.by("creadaEn").descending());

        assertThat(solicitudes.findBySolicitanteId(10L, orden).getTotalElements()).isEqualTo(2);
        assertThat(solicitudes.findBySolicitanteIdAndEstado(10L, EstadoSolicitud.LISTA, orden)).isEmpty();
        assertThat(solicitudes.findByEstado(EstadoSolicitud.EN_REVISION, orden).getTotalElements())
                .isGreaterThanOrEqualTo(3);
    }

    @Test
    void unItemDeOtraSolicitudNoSeEncuentra() {
        Solicitud a = solicitudes.saveAndFlush(solicitud(1L, 1));
        Solicitud b = solicitudes.saveAndFlush(solicitud(1L, 1));
        Long itemDeA = a.getItems().get(0).getId();

        assertThat(items.findByIdAndSolicitudId(itemDeA, a.getId())).isPresent();
        assertThat(items.findByIdAndSolicitudId(itemDeA, b.getId())).isEmpty();
    }

    private Solicitud solicitud(Long solicitanteId, int cantidadItems) {
        Solicitud s = new Solicitud();
        s.setSolicitanteId(solicitanteId);
        s.setSectorId(1L);
        s.setUrgencia(Urgencia.ALTA);
        s.setFechaNecesaria(LocalDate.now().plusDays(3));
        for (int i = 0; i < cantidadItems; i++) {
            Item item = new Item();
            item.setTipo(TipoItem.PRODUCTO);
            item.setCategoriaId(1L);
            item.setDetalle("Ítem " + i);
            item.setCantidad(new BigDecimal("2"));
            item.setPrecioEstimado(new BigDecimal("1500.50"));
            item.setMonedaEstimada(Moneda.ARS);
            s.agregarItem(item);
        }
        return s;
    }
}
