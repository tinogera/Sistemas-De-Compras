package com.Sistem.Solicitude_Compra.solicitudes.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoItem;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SolicitudDetalleResponse(
        Long id,
        Solicitante solicitante,
        String sector,
        Urgencia urgencia,
        LocalDate fechaNecesaria,
        String observaciones,
        EstadoSolicitud estado,
        LocalDateTime creadaEn,
        Long version,
        List<ItemDetalle> items) {

    public record Solicitante(Long id, String nombre) {
    }

    public record ItemDetalle(
            Long id,
            TipoItem tipo,
            Long categoriaId,
            String categoriaNombre,
            String detalle,
            BigDecimal cantidad,
            BigDecimal precioEstimado,
            Moneda monedaEstimada,
            BigDecimal precioTotal,
            Moneda monedaTotal,
            EstadoItem estado,
            EstadoAprobacion estadoAprobacion,
            String proveedorComprado,
            String motivoRechazo,
            String rechazadoPor,
            Long version) {
    }
}
