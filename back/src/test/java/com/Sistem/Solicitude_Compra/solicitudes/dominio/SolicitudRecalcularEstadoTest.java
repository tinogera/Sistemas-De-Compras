package com.Sistem.Solicitude_Compra.solicitudes.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import org.junit.jupiter.api.Test;

class SolicitudRecalcularEstadoTest {

    @Test
    void siTodosLosItemsEstanCanceladosLaSolicitudEsCancelada() {
        Solicitud solicitud = solicitud(item(EstadoItem.CANCELADO), item(EstadoItem.CANCELADO));

        solicitud.recalcularEstado();

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.CANCELADA);
    }

    @Test
    void compradosYRechazadosMezcladosDejanLaSolicitudLista() {
        Solicitud solicitud = solicitud(item(EstadoItem.COMPRADO), item(EstadoItem.RECHAZADO),
                item(EstadoItem.CANCELADO));

        solicitud.recalcularEstado();

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.LISTA);
    }

    @Test
    void siTodosFueronRechazadosLaSolicitudSeCierraEnseguida() {
        Solicitud solicitud = solicitud(item(EstadoItem.RECHAZADO), item(EstadoItem.RECHAZADO));

        solicitud.recalcularEstado();

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.CERRADA);
    }

    @Test
    void rechazadosYCanceladosSinComprarNadaTambienCierran() {
        Solicitud solicitud = solicitud(item(EstadoItem.RECHAZADO), item(EstadoItem.CANCELADO));

        solicitud.recalcularEstado();

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.CERRADA);
    }

    @Test
    void compradosSinOrdenEnviadaSigueLista() {
        Solicitud solicitud = solicitud(item(EstadoItem.COMPRADO), enviada(item(EstadoItem.COMPRADO)));

        solicitud.recalcularEstado();

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.LISTA);
    }

    @Test
    void compradosConOrdenEnviadaCierranLaSolicitud() {
        Solicitud solicitud = solicitud(enviada(item(EstadoItem.COMPRADO)), enviada(item(EstadoItem.COMPRADO)),
                item(EstadoItem.RECHAZADO));

        solicitud.recalcularEstado();

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.CERRADA);
    }

    @Test
    void unItemAprobadoPeroSinComprarMantieneLaSolicitudEnRevision() {
        Item aprobado = item(EstadoItem.EN_COTIZACION);
        aprobado.setEstadoAprobacion(EstadoAprobacion.APROBADA);
        Solicitud solicitud = solicitud(aprobado, item(EstadoItem.COMPRADO));

        solicitud.recalcularEstado();

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.EN_REVISION);
    }

    @Test
    void unItemPendienteMantieneLaSolicitudEnRevision() {
        Solicitud solicitud = solicitud(item(EstadoItem.PENDIENTE), item(EstadoItem.RECHAZADO));

        solicitud.recalcularEstado();

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.EN_REVISION);
    }

    @Test
    void unaSolicitudListaVuelveARevisionSiUnItemVuelveAEstarAbierto() {
        Solicitud solicitud = solicitud(item(EstadoItem.COMPRADO), item(EstadoItem.EN_COTIZACION));
        solicitud.setEstado(EstadoSolicitud.LISTA);

        solicitud.recalcularEstado();

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.EN_REVISION);
    }

    @Test
    void sinItemsNoCambiaElEstado() {
        Solicitud solicitud = solicitud();

        solicitud.recalcularEstado();

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.EN_REVISION);
    }

    private static Solicitud solicitud(Item... items) {
        Solicitud solicitud = new Solicitud();
        for (Item item : items) {
            solicitud.agregarItem(item);
        }
        return solicitud;
    }

    private static Item item(EstadoItem estado) {
        Item item = new Item();
        item.setEstado(estado);
        return item;
    }

    private static Item enviada(Item item) {
        item.setOrdenEnviada(true);
        return item;
    }
}
