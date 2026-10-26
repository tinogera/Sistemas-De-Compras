package com.Sistem.Solicitude_Compra.solicitudes.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitudResumenResponse(Long id, LocalDateTime creadaEn, Urgencia urgencia, LocalDate fechaNecesaria,
                                       EstadoSolicitud estado, int cantidadItems) {
}
