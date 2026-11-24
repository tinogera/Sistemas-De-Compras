package com.Sistem.Solicitude_Compra.integraciones.web;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.compras.ComprasApi;
import com.Sistem.Solicitude_Compra.compras.RecordatorioPendiente;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints que usa el workflow diario de n8n (se autentican con X-API-Key, ver SeguridadIntegracion). */
@RestController
@RequestMapping("/api/v1/integraciones")
public class RecordatoriosController {

    private final ComprasApi compras;
    private final AuthApi auth;

    public RecordatoriosController(ComprasApi compras, AuthApi auth) {
        this.compras = compras;
        this.auth = auth;
    }

    @GetMapping("/recordatorios-pendientes")
    public List<RecordatorioResponse> pendientes() {
        List<String> todosLosEncargados = auth.emailsEncargadosActivos();
        return compras.recordatoriosPendientes(Instant.now()).stream()
                .map(r -> new RecordatorioResponse(r.itemId(), r.solicitudId(), r.detalle(), r.urgencia().name(),
                        r.fechaNecesaria(), r.diasSinResolver(), destinatarios(r, todosLosEncargados)))
                .toList();
    }

    @PostMapping("/recordatorios/{itemId}/enviado")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enviado(@PathVariable Long itemId) {
        compras.registrarRecordatorio(itemId, Instant.now());
    }

    // El Encargado asignado; si nadie lo tomó (o ya no está activo), les llega a todos (caso borde 20).
    private List<String> destinatarios(RecordatorioPendiente r, List<String> todosLosEncargados) {
        if (r.encargadoId() == null) {
            return todosLosEncargados;
        }
        UsuarioResumen encargado = auth.obtenerUsuario(r.encargadoId());
        return encargado.activo() ? List.of(encargado.email()) : todosLosEncargados;
    }
}
