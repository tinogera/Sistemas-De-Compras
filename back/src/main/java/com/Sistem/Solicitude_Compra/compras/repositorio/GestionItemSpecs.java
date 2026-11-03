package com.Sistem.Solicitude_Compra.compras.repositorio;

import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class GestionItemSpecs {

    private static final List<EstadoGestion> ESTADOS_BANDEJA =
            List.of(EstadoGestion.PENDIENTE, EstadoGestion.EN_COTIZACION);

    private GestionItemSpecs() {
    }

    public static Specification<GestionItem> bandeja(Long encargadoId, Short prioridad, boolean paraComprar) {
        List<Specification<GestionItem>> filtros = new ArrayList<>();
        filtros.add(enBandeja());
        if (encargadoId != null) {
            filtros.add(conEncargado(encargadoId));
        }
        if (prioridad != null) {
            filtros.add(conPrioridad(prioridad));
        }
        if (paraComprar) {
            filtros.add(paraComprar());
        }
        return Specification.allOf(filtros);
    }

    private static Specification<GestionItem> enBandeja() {
        return (root, query, cb) -> root.get("estado").in(ESTADOS_BANDEJA);
    }

    private static Specification<GestionItem> conEncargado(Long encargadoId) {
        return (root, query, cb) -> cb.equal(root.get("encargadoId"), encargadoId);
    }

    private static Specification<GestionItem> conPrioridad(Short prioridad) {
        return (root, query, cb) -> cb.equal(root.get("prioridad"), prioridad);
    }

    private static Specification<GestionItem> paraComprar() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("estado"), EstadoGestion.EN_COTIZACION),
                cb.equal(root.get("estadoAprobacion"), EstadoAprobacion.APROBADA));
    }
}