package com.Sistem.Solicitude_Compra.compras.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BandejaItemResponse(
        Long itemId,
        Long solicitudId,
        TipoItem tipo,
        Long categoriaId,
        String categoriaNombre,
        String detalle,
        BigDecimal cantidad,
        Urgencia urgencia,
        LocalDate fechaNecesaria,
        BigDecimal precioEstimado,
        Moneda monedaEstimada,
        BigDecimal precioTotal,
        Moneda monedaTotal,
        EstadoGestion estado,
        EstadoAprobacion estadoAprobacion,
        Long encargadoId,
        String encargadoNombre,
        Long version) {
}