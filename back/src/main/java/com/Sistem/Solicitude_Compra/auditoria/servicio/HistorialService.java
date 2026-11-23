package com.Sistem.Solicitude_Compra.auditoria.servicio;

import com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EntradaHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.ItemSolicitud;
import com.Sistem.Solicitude_Compra.auditoria.repositorio.HistorialRepository;
import com.Sistem.Solicitude_Compra.auditoria.repositorio.ItemSolicitudRepository;
import com.Sistem.Solicitude_Compra.auditoria.web.HistorialResponse;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class HistorialService {

    private final HistorialRepository historial;
    private final ItemSolicitudRepository itemsSolicitud;
    private final AuthApi authApi;

    public HistorialService(HistorialRepository historial, ItemSolicitudRepository itemsSolicitud,
                            AuthApi authApi) {
        this.historial = historial;
        this.itemsSolicitud = itemsSolicitud;
        this.authApi = authApi;
    }

    /**
     * Encargado y Admin ven cualquier historial. El resto solo el de sus propias solicitudes (y los ítems de
     * esas solicitudes); cualquier otra entidad, o una solicitud ajena o desconocida, es 403.
     */
    public List<HistorialResponse> consultar(EntidadHistorial entidad, Long id, boolean incluirItems,
                                             Long usuarioId, Rol rol) {
        List<ItemSolicitud> items = entidad == EntidadHistorial.SOLICITUD
                ? itemsSolicitud.findBySolicitudId(id) : List.of();
        if (rol != Rol.ENCARGADO && rol != Rol.ADMIN) {
            verificarDueno(entidad, items, usuarioId);
        }

        List<EntradaHistorial> entradas = new ArrayList<>(
                historial.findByEntidadAndEntidadIdOrderByOcurridoEnAsc(entidad, id));
        if (incluirItems && !items.isEmpty()) {
            entradas.addAll(historial.findByEntidadAndEntidadIdInOrderByOcurridoEnAsc(EntidadHistorial.ITEM,
                    items.stream().map(ItemSolicitud::getItemId).toList()));
            entradas.sort(Comparator.comparing(EntradaHistorial::getOcurridoEn)
                    .thenComparing(EntradaHistorial::getId));
        }

        Map<Long, String> nombres = new HashMap<>();
        return entradas.stream().map(e -> new HistorialResponse(e.getOcurridoEn(), e.getEntidad(),
                e.getEntidadId(), e.getAccion(), e.getEstadoAnterior(), e.getEstadoNuevo(),
                nombreDe(e.getUsuarioId(), nombres), e.getComentario())).toList();
    }

    private void verificarDueno(EntidadHistorial entidad, List<ItemSolicitud> items, Long usuarioId) {
        if (entidad != EntidadHistorial.SOLICITUD || items.isEmpty()
                || items.stream().anyMatch(i -> !i.getSolicitanteId().equals(usuarioId))) {
            throw new SinPermisoException("No tenés permiso para ver este historial");
        }
    }

    private String nombreDe(Long usuarioId, Map<Long, String> cache) {
        if (usuarioId == null) {
            return "Sistema";
        }
        return cache.computeIfAbsent(usuarioId, id -> {
            try {
                return authApi.obtenerUsuario(id).nombre();
            } catch (NoEncontradoException ex) {
                return "Usuario " + id;
            }
        });
    }
}
