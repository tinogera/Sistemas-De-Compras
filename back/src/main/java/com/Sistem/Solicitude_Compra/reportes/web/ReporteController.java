package com.Sistem.Solicitude_Compra.reportes.web;

import com.Sistem.Solicitude_Compra.reportes.dominio.Agrupacion;
import com.Sistem.Solicitude_Compra.reportes.servicio.ReporteGastoService;
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

    public ReporteController(ReporteGastoService gasto) {
        this.gasto = gasto;
    }

    /** GET /api/v1/reportes/gasto?agrupar=sector|categoria|proveedor&desde=2026-11-01&hasta=2026-11-30 */
    @GetMapping("/gasto")
    public List<GastoResponse> gasto(@RequestParam String agrupar,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return gasto.gasto(Agrupacion.desde(agrupar), desde, hasta).stream().map(GastoResponse::de).toList();
    }
}
