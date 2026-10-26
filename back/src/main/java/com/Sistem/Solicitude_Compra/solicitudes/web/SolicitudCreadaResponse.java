package com.Sistem.Solicitude_Compra.solicitudes.web;

import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoItem;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import java.util.List;

public record SolicitudCreadaResponse(Long id, EstadoSolicitud estado, List<ItemCreadoResponse> items) {

    public record ItemCreadoResponse(Long id, EstadoItem estado) {
    }

    public static SolicitudCreadaResponse de(Solicitud s) {
        return new SolicitudCreadaResponse(s.getId(), s.getEstado(),
                s.getItems().stream().map(i -> new ItemCreadoResponse(i.getId(), i.getEstado())).toList());
    }
}
