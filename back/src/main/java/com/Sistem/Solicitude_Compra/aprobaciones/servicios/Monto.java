package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;

import java.math.BigDecimal;

/** Importe con su moneda. No se convierte entre monedas (decisión 8). */
public record Monto(BigDecimal importe, Moneda moneda) {
}
