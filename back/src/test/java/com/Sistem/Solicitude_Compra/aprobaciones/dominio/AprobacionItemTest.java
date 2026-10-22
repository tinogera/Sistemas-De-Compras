package com.Sistem.Solicitude_Compra.aprobaciones.dominio;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.EstadoPaso;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.TipoAprobador;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Test unitario puro de AprobacionItem.pasosActivos() (decisión 7): sin Spring ni base de datos. */
class AprobacionItemTest {

    @Test
    void dosPasosPendientesConElMismoOrdenSeDevuelvenJuntos() {
        AprobacionItem item = new AprobacionItem();
        PasoItem a = paso(1, EstadoPaso.PENDIENTE);
        PasoItem b = paso(1, EstadoPaso.PENDIENTE);
        PasoItem c = paso(2, EstadoPaso.PENDIENTE);
        item.agregarPaso(a);
        item.agregarPaso(b);
        item.agregarPaso(c);

        assertThat(item.pasosActivos()).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void tresPasosPendientesConElMismoOrdenSeDevuelvenTodos() {
        AprobacionItem item = new AprobacionItem();
        PasoItem a = paso(1, EstadoPaso.PENDIENTE);
        PasoItem b = paso(1, EstadoPaso.PENDIENTE);
        PasoItem c = paso(1, EstadoPaso.PENDIENTE);
        item.agregarPaso(a);
        item.agregarPaso(b);
        item.agregarPaso(c);

        assertThat(item.pasosActivos()).containsExactlyInAnyOrder(a, b, c);
    }

    @Test
    void siUnoDeLosPasosEnParaleloYaSeAprobo_soloQuedaElPendiente() {
        AprobacionItem item = new AprobacionItem();
        PasoItem aprobado = paso(1, EstadoPaso.APROBADO);
        PasoItem pendiente = paso(1, EstadoPaso.PENDIENTE);
        PasoItem siguiente = paso(2, EstadoPaso.PENDIENTE);
        item.agregarPaso(aprobado);
        item.agregarPaso(pendiente);
        item.agregarPaso(siguiente);

        assertThat(item.pasosActivos()).containsExactly(pendiente);
    }

    @Test
    void ignoraLosPasosNoPendientesAunqueTenganMenorOrden() {
        AprobacionItem item = new AprobacionItem();
        PasoItem aprobado = paso(1, EstadoPaso.APROBADO);
        PasoItem a = paso(2, EstadoPaso.PENDIENTE);
        PasoItem b = paso(2, EstadoPaso.PENDIENTE);
        PasoItem c = paso(3, EstadoPaso.PENDIENTE);
        item.agregarPaso(aprobado);
        item.agregarPaso(a);
        item.agregarPaso(b);
        item.agregarPaso(c);

        assertThat(item.pasosActivos()).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void sinPasosPendientesDevuelveListaVacia() {
        AprobacionItem item = new AprobacionItem();
        item.agregarPaso(paso(1, EstadoPaso.APROBADO));
        item.agregarPaso(paso(2, EstadoPaso.OMITIDO));
        item.agregarPaso(paso(3, EstadoPaso.CANCELADO));

        assertThat(item.pasosActivos()).isEmpty();
    }

    @Test
    void sinNingunPasoDevuelveListaVacia() {
        AprobacionItem item = new AprobacionItem();

        assertThat(item.pasosActivos()).isEmpty();
    }

    private static PasoItem paso(int orden, EstadoPaso estado) {
        PasoItem paso = new PasoItem();
        paso.setOrden(orden);
        paso.setTipoAprobador(TipoAprobador.ROL);
        paso.setRol(Rol.SUPERVISOR);
        paso.setNombreAprobador("Supervisor");
        paso.setEstado(estado);
        return paso;
    }
}