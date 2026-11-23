package com.Sistem.Solicitude_Compra.auditoria.repositorio;

import com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EntradaHistorial;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;

/** Solo guardar y leer: el historial no se edita ni se borra, por eso no extiende JpaRepository. */
public interface HistorialRepository extends Repository<EntradaHistorial, Long> {

    EntradaHistorial save(EntradaHistorial entrada);

    List<EntradaHistorial> findByEntidadAndEntidadIdOrderByOcurridoEnAsc(EntidadHistorial entidad, Long entidadId);

    List<EntradaHistorial> findByEntidadAndEntidadIdInOrderByOcurridoEnAsc(EntidadHistorial entidad,
                                                                           Collection<Long> entidadIds);
}
