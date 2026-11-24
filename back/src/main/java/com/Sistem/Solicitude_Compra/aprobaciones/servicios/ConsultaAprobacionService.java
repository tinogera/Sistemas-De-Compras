package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.AprobacionItem;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoCadena;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoPaso;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.PasoItem;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.AprobacionItemRepository;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.PasoItemRepository;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.ReglaRepository;
import com.Sistem.Solicitude_Compra.aprobaciones.web.AprobacionResponses.Detalle;
import com.Sistem.Solicitude_Compra.aprobaciones.web.AprobacionResponses.Fila;
import com.Sistem.Solicitude_Compra.aprobaciones.web.AprobacionResponses.PasoDetalle;
import com.Sistem.Solicitude_Compra.aprobaciones.web.AprobacionResponses.Resumen;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lo que ve cada aprobador: sus pendientes, lo que se viene, lo que decidió y el detalle de un ítem. */
@Service
@Transactional(readOnly = true)
public class ConsultaAprobacionService {

    private static final Pageable PRIMERA = PageRequest.of(0, 1);

    private final AprobacionItemRepository aprobaciones;
    private final PasoItemRepository pasos;
    private final ReglaRepository reglas;
    private final AuthApi auth;
    private final CatalogoApi catalogo;

    public ConsultaAprobacionService(AprobacionItemRepository aprobaciones, PasoItemRepository pasos,
                                     ReglaRepository reglas, AuthApi auth, CatalogoApi catalogo) {
        this.aprobaciones = aprobaciones;
        this.pasos = pasos;
        this.reglas = reglas;
        this.auth = auth;
        this.catalogo = catalogo;
    }

    /** {@code vista}: para-decidir, en-curso, decididas o todas (esta última, solo ADMIN). */
    public Page<Fila> listar(String vista, Long usuarioId, Rol rol, Long sectorId, Pageable pageable) {
        // El orden lo define cada consulta (urgencia y antigüedad), no el que pida el cliente.
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return switch (vista == null ? "para-decidir" : vista) {
            case "para-decidir" -> pasos.paraDecidir(usuarioId, rol, sectorId, TipoAprobador.ROL,
                    TipoAprobador.SECTOR, EstadoCadena.EN_CURSO, EstadoPaso.PENDIENTE, pagina)
                    .map(p -> fila(p.getItem(), p, usuarioId, rol, sectorId));
            case "en-curso" -> pasos.enCurso(rol, sectorId, TipoAprobador.ROL, TipoAprobador.SECTOR,
                    EstadoCadena.EN_CURSO, EstadoPaso.PENDIENTE, pagina)
                    .map(p -> fila(p.getItem(), p, usuarioId, rol, sectorId));
            case "decididas" -> pasos.decididasPor(usuarioId, pagina)
                    .map(p -> fila(p.getItem(), p, usuarioId, rol, sectorId));
            case "todas" -> {
                if (rol != Rol.ADMIN) {
                    throw new SinPermisoException("Solo un Admin puede ver todas las aprobaciones.");
                }
                yield aprobaciones.todasPorUrgencia(pagina).map(i -> {
                    List<PasoItem> activos = i.pasosActivos();
                    return fila(i, activos.isEmpty() ? null : activos.get(0), usuarioId, rol, sectorId);
                });
            }
            default -> throw new ParametroInvalidoException(
                    "La vista tiene que ser para-decidir, en-curso, decididas o todas.");
        };
    }

    public Detalle detalle(Long itemId, Long usuarioId, Rol rol, Long sectorId) {
        AprobacionItem item = aprobaciones.findById(itemId)
                .orElseThrow(() -> new NoEncontradoException("No hay aprobación para el ítem " + itemId));
        boolean tienePaso = item.getPasos().stream().anyMatch(p -> p.puedeDecidir(rol, sectorId));
        boolean puedeVer = Objects.equals(item.getSolicitanteId(), usuarioId) || rol == Rol.ENCARGADO
                || rol == Rol.ADMIN || tienePaso;
        if (!puedeVer) {
            throw new SinPermisoException("No podés ver la aprobación de este ítem.");
        }
        List<PasoItem> activos = item.pasosActivos();
        List<PasoDetalle> detalles = item.getPasos().stream().map(p -> new PasoDetalle(p.getId(), p.getOrden(),
                p.getNombreAprobador(), p.getEstado(),
                p.getDecididoPor() == null ? null : auth.obtenerUsuario(p.getDecididoPor()).nombre(),
                p.getDecididoEn(), p.getMotivo(), activos.contains(p),
                puedeDecidir(item, p, activos, usuarioId, rol, sectorId), p.getVersion())).toList();
        return new Detalle(item.getItemId(), item.getSolicitudId(), auth.obtenerUsuario(item.getSolicitanteId()).nombre(),
                auth.nombreSector(item.getSectorId()), catalogo.nombreCategoria(item.getCategoriaId()),
                item.getDetalle(), item.getCantidad(), item.getUrgencia(), item.getPrecioEstimado(),
                item.getMonedaEstimada(), item.getPrecioTotal(), item.getMonedaTotal(),
                item.getProveedorRazonSocial(), item.getReglaNombre(), item.getEstado(), detalles);
    }

    public Resumen resumen(Long usuarioId, Rol rol, Long sectorId) {
        boolean esAprobador = reglas.esAprobador(rol, sectorId, TipoAprobador.ROL, TipoAprobador.SECTOR);
        long paraDecidir = pasos.paraDecidir(usuarioId, rol, sectorId, TipoAprobador.ROL, TipoAprobador.SECTOR,
                EstadoCadena.EN_CURSO, EstadoPaso.PENDIENTE, PRIMERA).getTotalElements();
        return new Resumen(esAprobador, paraDecidir);
    }

    private Fila fila(AprobacionItem i, PasoItem paso, Long usuarioId, Rol rol, Long sectorId) {
        boolean puedo = paso != null && puedeDecidir(i, paso, i.pasosActivos(), usuarioId, rol, sectorId);
        return new Fila(i.getItemId(), i.getSolicitudId(), auth.obtenerUsuario(i.getSolicitanteId()).nombre(),
                auth.nombreSector(i.getSectorId()), catalogo.nombreCategoria(i.getCategoriaId()), i.getDetalle(),
                i.getCantidad(), i.getUrgencia(), i.getPrecioEstimado(), i.getMonedaEstimada(), i.getPrecioTotal(),
                i.getMonedaTotal(), i.getProveedorRazonSocial(), i.getReglaNombre(), i.getEstado(),
                paso == null ? null : paso.getId(), paso == null ? null : paso.getNombreAprobador(), puedo,
                paso == null ? null : paso.getVersion());
    }

    // Puede decidir si el ítem sigue en curso, el paso está activo, es de su rol o sector y no es un pedido suyo.
    private static boolean puedeDecidir(AprobacionItem item, PasoItem paso, List<PasoItem> activos, Long usuarioId,
                                        Rol rol, Long sectorId) {
        return item.getEstado() == EstadoCadena.EN_CURSO
                && paso.getEstado() == EstadoPaso.PENDIENTE
                && activos.contains(paso)
                && paso.puedeDecidir(rol, sectorId)
                && !Objects.equals(item.getSolicitanteId(), usuarioId);
    }
}
