package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Regla;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.ReglaCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.ReglaPaso;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.ReglaRepository;
import com.Sistem.Solicitude_Compra.aprobaciones.web.ReglaRequest;
import com.Sistem.Solicitude_Compra.aprobaciones.web.ReglaRequest.CondicionRequest;
import com.Sistem.Solicitude_Compra.aprobaciones.web.ReglaRequest.PasoRequest;
import com.Sistem.Solicitude_Compra.aprobaciones.web.ReglaResponse;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.eventos.ReglaModificada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ABM de reglas de aprobación (solo Admin). No se borran: se desactivan, porque los ítems en curso guardan el id
 * y el nombre de la regla que se les aplicó. Un cambio vale para los ítems nuevos y los que se re-evalúen.
 */
@Service
public class ReglaAdminService {

    private final ReglaRepository reglas;
    private final AuthApi auth;
    private final ApplicationEventPublisher eventos;

    public ReglaAdminService(ReglaRepository reglas, AuthApi auth, ApplicationEventPublisher eventos) {
        this.reglas = reglas;
        this.auth = auth;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<ReglaResponse> listar() {
        return reglas.findAllByOrderByPrioridadAscIdAsc().stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public ReglaResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    @Transactional
    public ReglaResponse crear(ReglaRequest request, Long usuarioId) {
        validarNombre(request.nombre(), null);
        boolean activa = request.activa() == null || request.activa();
        validarCondiciones(request.condiciones());
        validarPasos(request.pasos(), activa);

        Regla regla = new Regla();
        regla.setNombre(request.nombre().trim());
        regla.setPrioridad(request.prioridad());
        regla.setActiva(activa);
        regla.setCreadaPor(usuarioId);
        copiarCondicionesYPasos(regla, request);
        Regla guardada = reglas.save(regla);
        publicar(guardada, "CREADA", usuarioId);
        return aRespuesta(guardada);
    }

    @Transactional
    public ReglaResponse reemplazar(Long id, ReglaRequest request, Long usuarioId) {
        Regla regla = buscar(id);
        if (request.version() == null || !request.version().equals(regla.getVersion())) {
            throw new ConflictoException("CONFLICTO_VERSION", "Otro usuario modificó esta regla. Recargá la página.");
        }
        validarNombre(request.nombre(), id);
        boolean activa = request.activa() == null ? regla.isActiva() : request.activa();
        if (regla.isPorDefecto()) {
            if (!activa) {
                throw new ReglaNegocioException("REGLA_POR_DEFECTO_NO_DESACTIVABLE",
                        "La regla por defecto no se puede desactivar.");
            }
            if (request.condiciones() != null && !request.condiciones().isEmpty()) {
                throw new ReglaNegocioException("REGLA_POR_DEFECTO_SIN_CONDICIONES",
                        "La regla por defecto no puede tener condiciones.");
            }
            if (request.pasos() == null || request.pasos().isEmpty()) {
                throw new ReglaNegocioException("REGLA_POR_DEFECTO_SIN_PASOS",
                        "La regla por defecto tiene que tener al menos un paso.");
            }
        }
        validarCondiciones(request.condiciones());
        validarPasos(request.pasos(), activa);

        regla.setNombre(request.nombre().trim());
        regla.setPrioridad(request.prioridad());
        regla.setActiva(activa);
        regla.setModificadaPor(usuarioId);
        regla.setModificadaEn(Instant.now());
        regla.getCondiciones().clear();
        regla.getPasos().clear();
        // Se fuerza el borrado de los hijos viejos antes de insertar los nuevos.
        reglas.saveAndFlush(regla);
        copiarCondicionesYPasos(regla, request);
        Regla guardada = reglas.save(regla);
        publicar(guardada, "EDITADA", usuarioId);
        return aRespuesta(guardada);
    }

    @Transactional
    public ReglaResponse activar(Long id, Long usuarioId) {
        Regla regla = buscar(id);
        if (!regla.isActiva()) {
            exigirAprobadores(regla.getPasos().stream().map(p -> new PasoRequest(p.getOrden(), p.getTipoAprobador(),
                    p.getRol(), p.getSectorId())).toList());
            regla.setActiva(true);
            registrarModificacion(regla, usuarioId);
            publicar(regla, "ACTIVADA", usuarioId);
        }
        return aRespuesta(regla);
    }

    @Transactional
    public ReglaResponse desactivar(Long id, Long usuarioId) {
        Regla regla = buscar(id);
        if (regla.isPorDefecto()) {
            throw new ReglaNegocioException("REGLA_POR_DEFECTO_NO_DESACTIVABLE",
                    "La regla por defecto no se puede desactivar.");
        }
        if (regla.isActiva()) {
            regla.setActiva(false);
            registrarModificacion(regla, usuarioId);
            publicar(regla, "DESACTIVADA", usuarioId);
        }
        return aRespuesta(regla);
    }

    // ---- validaciones ----

    private void validarNombre(String nombre, Long idActual) {
        String limpio = nombre.trim();
        boolean repetido = idActual == null
                ? reglas.existsByNombreIgnoreCase(limpio)
                : reglas.existsByNombreIgnoreCaseAndIdNot(limpio, idActual);
        if (repetido) {
            throw new ConflictoException("NOMBRE_DUPLICADO", "Ya existe una regla con ese nombre.");
        }
    }

    private void validarCondiciones(List<CondicionRequest> condiciones) {
        for (CondicionRequest c : condiciones == null ? List.<CondicionRequest>of() : condiciones) {
            if (c.tipo() != TipoCondicion.MONTO) {
                throw new ReglaNegocioException("CONDICION_NO_SOPORTADA",
                        "Por ahora solo hay condiciones de tipo MONTO.");
            }
            if (c.valor() == null || c.valor().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ReglaNegocioException("CONDICION_INVALIDA", "El valor de la condición tiene que ser mayor a 0.");
            }
            if (c.moneda() == null) {
                throw new ReglaNegocioException("CONDICION_INVALIDA", "La condición de monto necesita una moneda.");
            }
        }
    }

    private void validarPasos(List<PasoRequest> pasos, boolean activa) {
        List<PasoRequest> lista = pasos == null ? List.of() : pasos;
        // Los órdenes tienen que ser 1, 2, 3... sin saltos (varios pasos con el mismo orden van en paralelo).
        Set<Integer> ordenes = new HashSet<>();
        lista.forEach(p -> ordenes.add(p.orden()));
        for (int i = 1; i <= ordenes.size(); i++) {
            if (!ordenes.contains(i)) {
                throw new ReglaNegocioException("ORDEN_INVALIDO",
                        "El orden de los pasos tiene que empezar en 1 y no tener saltos (1, 2, 3...).");
            }
        }
        Set<String> vistos = new HashSet<>();
        for (PasoRequest p : lista) {
            if (p.tipoAprobador() == TipoAprobador.ROL) {
                if (p.rol() == null || p.sectorId() != null) {
                    throw new ReglaNegocioException("PASO_INVALIDO", "Un paso de rol necesita el rol y no un sector.");
                }
                if (p.rol() == Rol.SOLICITANTE) {
                    throw new ReglaNegocioException("ROL_NO_APROBADOR", "El rol SOLICITANTE no puede aprobar.");
                }
            } else {
                if (p.sectorId() == null || p.rol() != null) {
                    throw new ReglaNegocioException("PASO_INVALIDO", "Un paso de sector necesita el sector y no un rol.");
                }
                if (!auth.existeSector(p.sectorId())) {
                    throw new ReglaNegocioException("SECTOR_INEXISTENTE", "No existe el sector " + p.sectorId() + ".");
                }
            }
            String clave = p.tipoAprobador() + ":" + (p.rol() != null ? p.rol() : p.sectorId());
            if (!vistos.add(clave)) {
                throw new ReglaNegocioException("APROBADOR_REPETIDO", "El mismo aprobador no puede repetirse en una regla.");
            }
        }
        if (activa) {
            exigirAprobadores(lista);
        }
    }

    // Cada paso tiene que tener a alguien activo que lo pueda decidir (solo se exige en reglas activas).
    private void exigirAprobadores(List<PasoRequest> pasos) {
        for (PasoRequest p : pasos) {
            if (sinAprobadores(p.tipoAprobador(), p.rol(), p.sectorId())) {
                throw new ReglaNegocioException("SIN_APROBADORES",
                        "Nadie activo puede decidir el paso " + p.orden() + " (" + nombre(p.tipoAprobador(), p.rol(),
                                p.sectorId()) + ").");
            }
        }
    }

    private boolean sinAprobadores(TipoAprobador tipo, Rol rol, Long sectorId) {
        return tipo == TipoAprobador.ROL ? !auth.hayUsuariosActivosConRol(rol) : !auth.hayUsuariosActivosEnSector(sectorId);
    }

    private String nombre(TipoAprobador tipo, Rol rol, Long sectorId) {
        return tipo == TipoAprobador.ROL ? NombreAprobador.de(rol) : auth.nombreSector(sectorId);
    }

    // ---- armado ----

    private void copiarCondicionesYPasos(Regla regla, ReglaRequest request) {
        for (CondicionRequest c : request.condiciones() == null ? List.<CondicionRequest>of() : request.condiciones()) {
            ReglaCondicion condicion = new ReglaCondicion();
            condicion.setTipo(c.tipo());
            condicion.setOperador(c.operador());
            condicion.setValorNumerico(c.valor());
            condicion.setMoneda(c.moneda());
            regla.agregarCondicion(condicion);
        }
        for (PasoRequest p : request.pasos() == null ? List.<PasoRequest>of() : request.pasos()) {
            ReglaPaso paso = new ReglaPaso();
            paso.setOrden(p.orden());
            paso.setTipoAprobador(p.tipoAprobador());
            paso.setRol(p.rol());
            paso.setSectorId(p.sectorId());
            regla.agregarPaso(paso);
        }
    }

    private Regla buscar(Long id) {
        return reglas.findById(id).orElseThrow(() -> new NoEncontradoException("No existe la regla " + id));
    }

    private void registrarModificacion(Regla regla, Long usuarioId) {
        regla.setModificadaPor(usuarioId);
        regla.setModificadaEn(Instant.now());
        reglas.save(regla);
    }

    private void publicar(Regla regla, String accion, Long usuarioId) {
        eventos.publishEvent(new ReglaModificada(UUID.randomUUID(), Instant.now(), regla.getId(), regla.getNombre(),
                accion, usuarioId));
    }

    private ReglaResponse aRespuesta(Regla regla) {
        List<ReglaResponse.CondicionResponse> condiciones = regla.getCondiciones().stream()
                .map(c -> new ReglaResponse.CondicionResponse(c.getTipo(), c.getOperador(), c.getValorNumerico(),
                        c.getMoneda())).toList();
        List<ReglaResponse.PasoResponse> pasos = regla.getPasos().stream()
                .sorted(Comparator.comparingInt(ReglaPaso::getOrden))
                .map(p -> new ReglaResponse.PasoResponse(p.getOrden(), p.getTipoAprobador(), p.getRol(),
                        p.getSectorId(), nombre(p.getTipoAprobador(), p.getRol(), p.getSectorId()),
                        sinAprobadores(p.getTipoAprobador(), p.getRol(), p.getSectorId()))).toList();
        return new ReglaResponse(regla.getId(), regla.getNombre(), regla.getPrioridad(), regla.isActiva(),
                regla.isPorDefecto(), regla.getVersion(), condiciones, pasos);
    }
}
