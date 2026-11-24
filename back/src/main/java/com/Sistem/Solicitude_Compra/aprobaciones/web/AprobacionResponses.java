package com.Sistem.Solicitude_Compra.aprobaciones.web;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoCadena;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoPaso;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Respuestas de las consultas de aprobaciones (T23.6). */
public final class AprobacionResponses {

    private AprobacionResponses() {
    }

    /** Una fila de la lista del aprobador. {@code pasoId}, {@code aprobadorNombre} y {@code version} son del paso. */
    public record Fila(
            Long itemId,
            Long solicitudId,
            String solicitanteNombre,
            String sector,
            String categoriaNombre,
            String detalle,
            BigDecimal cantidad,
            Urgencia urgencia,
            BigDecimal precioEstimado,
            Moneda monedaEstimada,
            BigDecimal precioTotal,
            Moneda monedaTotal,
            String proveedor,
            String reglaNombre,
            EstadoCadena estado,
            Long pasoId,
            String aprobadorNombre,
            boolean puedoDecidir,
            Integer version) {
    }

    public record PasoDetalle(
            Long id,
            int orden,
            String aprobadorNombre,
            EstadoPaso estado,
            String decididoPorNombre,
            Instant decididoEn,
            String motivo,
            boolean activo,
            boolean puedoDecidir,
            Integer version) {
    }

    public record Detalle(
            Long itemId,
            Long solicitudId,
            String solicitanteNombre,
            String sector,
            String categoriaNombre,
            String detalle,
            BigDecimal cantidad,
            Urgencia urgencia,
            BigDecimal precioEstimado,
            Moneda monedaEstimada,
            BigDecimal precioTotal,
            Moneda monedaTotal,
            String proveedor,
            String reglaNombre,
            EstadoCadena estado,
            List<PasoDetalle> pasos) {
    }

    public record Resumen(boolean esAprobador, long paraDecidir) {
    }
}
