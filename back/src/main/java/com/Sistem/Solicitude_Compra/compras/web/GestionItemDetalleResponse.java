package com.Sistem.Solicitude_Compra.compras.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record GestionItemDetalleResponse(
        Long itemId,
        Long solicitudId,
        TipoItem tipo,
        Long categoriaId,
        String categoriaNombre,
        String detalle,
        BigDecimal cantidad,
        Urgencia urgencia,
        LocalDate fechaNecesaria,
        OffsetDateTime solicitadoEn,
        BigDecimal precioEstimado,
        Moneda monedaEstimada,
        BigDecimal precioTotal,
        Moneda monedaTotal,
        BigDecimal montoEnvio,
        EstadoGestion estado,
        EstadoAprobacion estadoAprobacion,
        Long encargadoId,
        String encargadoNombre,
        Long cotizacionElegidaId,
        Long version,
        boolean todasVencidas,
        List<CotizacionResponse> cotizaciones) {

}