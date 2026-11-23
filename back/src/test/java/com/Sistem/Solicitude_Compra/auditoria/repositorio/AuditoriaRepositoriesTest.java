package com.Sistem.Solicitude_Compra.auditoria.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EntradaHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EventoProcesado;
import com.Sistem.Solicitude_Compra.auditoria.dominio.ItemSolicitud;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AuditoriaRepositoriesTest {

    @Autowired HistorialRepository historial;
    @Autowired ItemSolicitudRepository items;
    @Autowired EventoProcesadoRepository procesados;
    @Autowired EntityManager em;

    private EntradaHistorial entrada(EntidadHistorial entidad, Long id, Instant cuando) {
        return EntradaHistorial.de(entidad, id, AccionHistorial.CAMBIO_ESTADO, "PENDIENTE", "CANCELADO", 7L,
                "comentario", cuando);
    }

    @Test
    void guardaLaEntradaConTodosSusCampos() {
        Instant cuando = Instant.parse("2026-11-01T10:00:00Z");
        EntradaHistorial guardada = historial.save(entrada(EntidadHistorial.ITEM, 1L, cuando));
        em.flush();
        em.clear();

        EntradaHistorial leida = historial
                .findByEntidadAndEntidadIdOrderByOcurridoEnAsc(EntidadHistorial.ITEM, 1L).get(0);

        assertThat(leida.getId()).isEqualTo(guardada.getId());
        assertThat(leida.getAccion()).isEqualTo(AccionHistorial.CAMBIO_ESTADO);
        assertThat(leida.getEstadoAnterior()).isEqualTo("PENDIENTE");
        assertThat(leida.getEstadoNuevo()).isEqualTo("CANCELADO");
        assertThat(leida.getUsuarioId()).isEqualTo(7L);
        assertThat(leida.getComentario()).isEqualTo("comentario");
        assertThat(leida.getOcurridoEn()).isEqualTo(cuando);
    }

    @Test
    void lasEntradasDeUnaEntidadVienenOrdenadasPorFechaYSinMezclarOtras() {
        historial.save(entrada(EntidadHistorial.ITEM, 2L, Instant.parse("2026-11-01T12:00:00Z")));
        historial.save(entrada(EntidadHistorial.ITEM, 2L, Instant.parse("2026-11-01T09:00:00Z")));
        historial.save(entrada(EntidadHistorial.ITEM, 3L, Instant.parse("2026-11-01T10:00:00Z")));
        historial.save(entrada(EntidadHistorial.SOLICITUD, 2L, Instant.parse("2026-11-01T10:00:00Z")));
        em.flush();
        em.clear();

        var lista = historial.findByEntidadAndEntidadIdOrderByOcurridoEnAsc(EntidadHistorial.ITEM, 2L);

        assertThat(lista).extracting(EntradaHistorial::getOcurridoEn)
                .containsExactly(Instant.parse("2026-11-01T09:00:00Z"), Instant.parse("2026-11-01T12:00:00Z"));
    }

    @Test
    void elHistorialNoExponeBorradoNiEdicion() {
        assertThat(Arrays.stream(HistorialRepository.class.getMethods()).map(Method::getName))
                .noneMatch(nombre -> nombre.startsWith("delete") || nombre.startsWith("remove"))
                .containsExactlyInAnyOrder("save", "findByEntidadAndEntidadIdOrderByOcurridoEnAsc");
        assertThat(Arrays.stream(EntradaHistorial.class.getMethods()).map(Method::getName))
                .noneMatch(nombre -> nombre.startsWith("set"));
    }

    @Test
    void unaEntradaSinEntidadOSinFechaSeRechaza() {
        assertThatThrownBy(() -> EntradaHistorial.de(null, 1L, AccionHistorial.CREACION, null, null, null, null,
                Instant.now())).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> EntradaHistorial.de(EntidadHistorial.ITEM, 1L, AccionHistorial.CREACION, null,
                null, null, null, null)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void buscaLosItemsDeUnaSolicitud() {
        items.save(new ItemSolicitud(10L, 1L, 5L));
        items.save(new ItemSolicitud(11L, 1L, 5L));
        items.save(new ItemSolicitud(12L, 2L, 6L));
        em.flush();
        em.clear();

        assertThat(items.findBySolicitudId(1L)).extracting(ItemSolicitud::getItemId).containsExactlyInAnyOrder(10L, 11L);
        assertThat(items.findBySolicitudId(1L)).allMatch(i -> i.getSolicitanteId().equals(5L));
        assertThat(items.findBySolicitudId(99L)).isEmpty();
    }

    @Test
    void recuerdaLosEventosYaProcesados() {
        UUID id = UUID.randomUUID();
        assertThat(procesados.existsById(id)).isFalse();

        procesados.saveAndFlush(new EventoProcesado(id));

        assertThat(procesados.existsById(id)).isTrue();
    }
}
