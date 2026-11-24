package com.Sistem.Solicitude_Compra.integraciones.servicio;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.eventos.EncargadoDesasignado;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudLista;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Convierte los eventos del sistema en avisos para n8n, que es quien manda los mails. */
@Component
public class AvisosN8n {

    private static final String CANCELADO = "CANCELADO";

    private final ClienteN8n n8n;
    private final AuthApi auth;
    private final CatalogoApi catalogo;

    public AvisosN8n(ClienteN8n n8n, AuthApi auth, CatalogoApi catalogo) {
        this.n8n = n8n;
        this.auth = auth;
        this.catalogo = catalogo;
    }

    @ApplicationModuleListener
    void alCrearseLaSolicitud(SolicitudCreada e) {
        UsuarioResumen solicitante = auth.obtenerUsuario(e.solicitanteId());
        n8n.enviar("solicitud-creada", new SolicitudNueva(e.solicitudId(), solicitante.nombre(),
                auth.nombreSector(e.sectorId()), e.urgencia().name(), e.fechaNecesaria(), e.items().size(),
                auth.emailsEncargadosActivos()));
    }

    @ApplicationModuleListener
    void alQuedarListaLaSolicitud(SolicitudLista e) {
        UsuarioResumen solicitante = auth.obtenerUsuario(e.solicitanteId());
        // Los ítems que el solicitante canceló no se avisan: ya lo sabe.
        List<ItemResultado> items = e.resultados().stream()
                .filter(r -> !CANCELADO.equals(r.estado()))
                .map(r -> new ItemResultado(
                        r.categoriaId() == null ? null : catalogo.nombreCategoria(r.categoriaId()),
                        r.detalle(), r.estado(), r.proveedorRazonSocial(), r.motivoRechazo(), r.rechazadoPor()))
                .toList();
        n8n.enviar("solicitud-lista", new ResultadoSolicitud(e.solicitudId(), solicitante.email(),
                solicitante.nombre(), items));
    }

    @ApplicationModuleListener
    void alDesasignarseUnEncargado(EncargadoDesasignado e) {
        UsuarioResumen desasignado = auth.obtenerUsuario(e.usuarioId());
        List<String> destinatarios = auth.emailsEncargadosActivos().stream()
                .filter(email -> !Objects.equals(email, desasignado.email())).toList();
        n8n.enviar("encargado-desasignado", new EncargadoQuitado(desasignado.nombre(), destinatarios));
    }

    record SolicitudNueva(Long solicitudId, String solicitanteNombre, String sector, String urgencia,
                          LocalDate fechaNecesaria, int cantidadItems, List<String> emailsEncargados) {
    }

    record ItemResultado(String categoria, String detalle, String resultado, String proveedor,
                         String motivoRechazo, String rechazadoPor) {
    }

    record ResultadoSolicitud(Long solicitudId, String solicitanteEmail, String solicitanteNombre,
                              List<ItemResultado> items) {
    }

    record EncargadoQuitado(String encargadoNombre, List<String> emailsEncargados) {
    }
}
