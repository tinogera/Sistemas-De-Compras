package com.Sistem.Solicitude_Compra.auditoria.web;

import com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial;

import java.time.Instant;

public record HistorialResponse(
        Instant ocurridoEn,
        EntidadHistorial entidad,
        Long entidadId,
        AccionHistorial accion,
        String estadoAnterior,
        String estadoNuevo,
        String usuarioNombre,
        String comentario) {
}
