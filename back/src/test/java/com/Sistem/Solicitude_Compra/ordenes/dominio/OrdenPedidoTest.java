package com.Sistem.Solicitude_Compra.ordenes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;

class OrdenPedidoTest {

    @Test
    void calculaElPrecioUnitarioRedondeado() {
        OrdenPedido orden = orden("3", "100.00");

        assertThat(orden.getPrecioUnitario()).isEqualByComparingTo("33.33");
    }

    @Test
    void rechazaCantidadCero() {
        assertThatThrownBy(() -> orden("0", "100.00"))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void rechazaPrecioTotalNegativo() {
        assertThatThrownBy(() -> orden("2", "-5.00"))
                .isInstanceOf(ReglaNegocioException.class);
    }

    private static OrdenPedido orden(String cantidad, String total) {
        return new OrdenPedido(1L, 10L, 20L, 30L, "Proveedor SA", null, TipoItem.PRODUCTO,
                "detalle", new BigDecimal(cantidad), Moneda.ARS, new BigDecimal(total));
    }
}
