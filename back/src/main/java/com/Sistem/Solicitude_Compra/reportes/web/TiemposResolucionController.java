package com.Sistem.Solicitude_Compra.reportes.web;

import com.Sistem.Solicitude_Compra.reportes.servicio.ReporteTiemposService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@PreAuthorize("hasAnyRole('ENCARGADO','ADMIN')")
@RestController
@RequestMapping("/api/v1/reportes")
public class TiemposResolucionController {

    private final ReporteTiemposService tiempos;

    public TiemposResolucionController(ReporteTiemposService tiempos) {
        this.tiempos = tiempos;
    }

    /** GET /api/v1/reportes/tiempos-resolucion?desde=2026-11-01&hasta=2026-11-30 */
    @GetMapping("/tiempos-resolucion")
    public TiemposResolucionResponse tiemposResolucion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return TiemposResolucionResponse.de(tiempos.tiempos(desde, hasta));
    }
}
