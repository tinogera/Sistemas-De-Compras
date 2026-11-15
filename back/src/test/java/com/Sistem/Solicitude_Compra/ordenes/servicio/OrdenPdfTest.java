package com.Sistem.Solicitude_Compra.ordenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;

class OrdenPdfTest {

    OrdenPdf pdf = new OrdenPdf();

    @Test
    void generaUnPdfValido() {
        byte[] contenido = pdf.generar(orden());

        assertTrue(contenido.length > 500);
        assertEquals("%PDF", new String(Arrays.copyOf(contenido, 4), StandardCharsets.US_ASCII));
    }

    @Test
    void generaElPdfTambienConMontoDeEnvioYSinDetalle() {
        OrdenPedido orden = orden();
        ReflectionTestUtils.setField(orden, "montoEnvio", new BigDecimal("5000.00"));
        ReflectionTestUtils.setField(orden, "detalle", null);

        byte[] contenido = pdf.generar(orden);

        assertEquals("%PDF", new String(Arrays.copyOf(contenido, 4), StandardCharsets.US_ASCII));
    }

    private static OrdenPedido orden() {
        return new OrdenPedido(7L, 10L, 20L, 30L, "Papelera SA", null, TipoItem.PRODUCTO,
                "Lapiceras", new BigDecimal("20"), Moneda.ARS, new BigDecimal("1000.00"));
    }
}
