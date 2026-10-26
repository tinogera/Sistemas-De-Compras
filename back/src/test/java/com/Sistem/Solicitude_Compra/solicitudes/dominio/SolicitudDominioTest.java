package com.Sistem.Solicitude_Compra.solicitudes.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SolicitudDominioTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void unaSolicitudSinItemsNoEsValida() {
        assertThat(validador.validate(solicitud())).isNotEmpty();
    }

    @Test
    void unaSolicitudConUnItemValidoEsValida() {
        Solicitud solicitud = solicitud();
        solicitud.agregarItem(item("2", "1000"));

        assertThat(validador.validate(solicitud)).isEmpty();
        assertThat(solicitud.getItems().get(0).getSolicitud()).isSameAs(solicitud);
    }

    @Test
    void unItemConCantidadCeroNoEsValido() {
        assertThat(validador.validate(item("0", "1000"))).isNotEmpty();
    }

    @Test
    void unItemSinPrecioEstimadoNoEsValido() {
        Item item = item("1", "1000");
        item.setPrecioEstimado(null);
        item.setSolicitud(solicitud());

        assertThat(validador.validate(item)).isNotEmpty();
    }

    @Test
    void unItemConPrecioEstimadoCeroNoEsValido() {
        Item item = item("1", "0");
        item.setSolicitud(solicitud());

        assertThat(validador.validate(item)).isNotEmpty();
    }

    @Test
    void soloSePuedeEditarUnItemPendiente() {
        Item item = item("1", "10");
        assertThat(item.puedeEditarse()).isTrue();

        item.setEstado(EstadoItem.EN_COTIZACION);
        assertThat(item.puedeEditarse()).isFalse();
    }

    @Test
    void soloSePuedeCancelarSiTodosLosItemsEstanPendientes() {
        Solicitud solicitud = solicitud();
        solicitud.agregarItem(item("1", "10"));
        solicitud.agregarItem(item("1", "10"));
        assertThat(solicitud.puedeCancelarse()).isTrue();

        solicitud.getItems().get(1).setEstado(EstadoItem.EN_COTIZACION);
        assertThat(solicitud.puedeCancelarse()).isFalse();
    }

    @Test
    void losEstadosFinalesSonCompradoRechazadoYCancelado() {
        assertThat(EstadoItem.COMPRADO.esFinal()).isTrue();
        assertThat(EstadoItem.RECHAZADO.esFinal()).isTrue();
        assertThat(EstadoItem.CANCELADO.esFinal()).isTrue();
        assertThat(EstadoItem.PENDIENTE.esFinal()).isFalse();
        assertThat(EstadoItem.EN_COTIZACION.esFinal()).isFalse();
    }

    static Solicitud solicitud() {
        Solicitud s = new Solicitud();
        s.setSolicitanteId(1L);
        s.setSectorId(1L);
        s.setUrgencia(Urgencia.MEDIA);
        s.setFechaNecesaria(LocalDate.now().plusDays(5));
        return s;
    }

    static Item item(String cantidad, String precio) {
        Item i = new Item();
        i.setTipo(TipoItem.PRODUCTO);
        i.setCategoriaId(1L);
        i.setDetalle("Resmas A4");
        i.setCantidad(new BigDecimal(cantidad));
        i.setPrecioEstimado(new BigDecimal(precio));
        i.setMonedaEstimada(Moneda.ARS);
        return i;
    }
}
