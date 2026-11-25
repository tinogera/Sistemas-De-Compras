package com.Sistem.Solicitude_Compra.reportes.web;

import com.Sistem.Solicitude_Compra.reportes.dominio.Agrupacion;
import com.Sistem.Solicitude_Compra.reportes.servicio.ReporteGastoService;
import com.Sistem.Solicitude_Compra.reportes.servicio.ReporteTiemposService;
import com.Sistem.Solicitude_Compra.reportes.servicio.TiemposResolucion;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@PreAuthorize("hasAnyRole('ENCARGADO','ADMIN')")
@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {

    private final ReporteGastoService gasto;
    private final ReporteTiemposService tiempos;

    public ReporteController(ReporteGastoService gasto, ReporteTiemposService tiempos) {
        this.gasto = gasto;
        this.tiempos = tiempos;
    }

    /** GET /api/v1/reportes/gasto?agrupar=sector|categoria|proveedor&desde=2026-11-01&hasta=2026-11-30 */
    @GetMapping("/gasto")
    public List<GastoResponse> gasto(@RequestParam String agrupar,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return gasto.gasto(Agrupacion.desde(agrupar), desde, hasta).stream().map(GastoResponse::de).toList();
    }

    /** GET /api/v1/reportes/tiempos-resolucion?desde=2026-11-01&hasta=2026-11-30 */
    @GetMapping("/tiempos-resolucion")
    public TiemposResolucion tiemposResolucion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return tiempos.tiempos(desde, hasta);
    }
}
