package com.Sistem.Solicitude_Compra.integraciones.web;

import java.time.LocalDate;
import java.util.List;

public record RecordatorioResponse(Long itemId, Long solicitudId, String detalle, String urgencia,
                                   LocalDate fechaNecesaria, long diasSinResolver, List<String> destinatarios) {
}
