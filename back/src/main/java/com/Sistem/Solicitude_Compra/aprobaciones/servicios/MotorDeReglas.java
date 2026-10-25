package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.TipoCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Regla;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.ReglaCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.ReglaPaso;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.ReglaRepository;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Elige qué regla de aprobación aplica a un ítem y qué pasos exige.
 * Solo lee las reglas: no guarda nada.
 */
@Service
public class MotorDeReglas {

    private final ReglaRepository reglaRepository;
    private final Map<TipoCondicion, EvaluadorCondicion> evaluadores = new EnumMap<>(TipoCondicion.class);

    public MotorDeReglas(ReglaRepository reglaRepository, List<EvaluadorCondicion> evaluadores) {
        this.reglaRepository = reglaRepository;
        for (EvaluadorCondicion evaluador : evaluadores) {
            EvaluadorCondicion repetido = this.evaluadores.put(evaluador.tipo(), evaluador);
            if (repetido != null) {
                throw new IllegalStateException(
                        "Hay más de un evaluador para el tipo de condición " + evaluador.tipo());
            }
        }
    }

    /**
     * Recorre las reglas activas por prioridad ascendente y devuelve la primera que se cumple.
     * Una regla sin condiciones (la por defecto) siempre se cumple.
     * Una regla sin pasos devuelve una lista vacía: aprobación automática.
     */
    @Transactional(readOnly = true)
    public ResultadoEvaluacion evaluar(ContextoEvaluacion contexto) {
        for (Regla regla : reglaRepository.findByActivaTrueOrderByPrioridadAsc()) {
            if (cumple(regla, contexto)) {
                return new ResultadoEvaluacion(regla, pasosRequeridos(regla));
            }
        }
        throw new IllegalStateException(
                "Ninguna regla activa se cumple para el ítem. Debe existir y estar activa la regla "
                        + "por defecto (sin condiciones): revisar la configuración de reglas de aprobación.");
    }

    /** Se cumple si todas sus condiciones se cumplen; sin condiciones, siempre. */
    private boolean cumple(Regla regla, ContextoEvaluacion contexto) {
        return regla.getCondiciones().stream()
                .allMatch(condicion -> evaluadorPara(condicion).cumple(condicion, contexto));
    }

    private EvaluadorCondicion evaluadorPara(ReglaCondicion condicion) {
        EvaluadorCondicion evaluador = evaluadores.get(condicion.getTipo());
        if (evaluador == null) {
            throw new IllegalStateException(
                    "No hay un evaluador para el tipo de condición " + condicion.getTipo());
        }
        return evaluador;
    }

    private List<PasoRequerido> pasosRequeridos(Regla regla) {
        return regla.getPasos().stream()
                .sorted(Comparator.comparingInt(ReglaPaso::getOrden))
                .map(paso -> new PasoRequerido(
                        paso.getOrden(),
                        paso.getTipoAprobador(),
                        paso.getRol(),
                        paso.getSectorId(),
                        nombreDe(paso)))
                .toList();
    }

    private String nombreDe(ReglaPaso paso) {
        return switch (paso.getTipoAprobador()) {
            case ROL -> nombreDeRol(paso.getRol());
            // Provisorio: el nombre real del sector sale de AuthApi.nombreSector.
            case SECTOR -> "Sector " + paso.getSectorId();
        };
    }

    private String nombreDeRol(Rol rol) {
        return switch (rol) {
            case SOLICITANTE -> "Solicitante";
            case ENCARGADO -> "Encargado";
            case ADMIN -> "Administrador";
            case SUPERVISOR -> "Supervisor";
        };
    }
}