package com.Sistem.Solicitude_Compra.aprobaciones.repository;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Regla;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReglaRepository extends JpaRepository<Regla, Long> {

    /**
     * Reglas activas por prioridad ascendente (la de menor número se evalúa primero).
     * El EntityGraph trae condiciones y pasos en una sola consulta, sin N+1.
     */
    @EntityGraph(attributePaths = {"condiciones", "pasos"})
    List<Regla> findByActivaTrueOrderByPrioridadAsc();

    /** Todas las reglas (activas o no) por prioridad, con condiciones y pasos en una sola consulta. */
    @EntityGraph(attributePaths = {"condiciones", "pasos"})
    List<Regla> findAllByOrderByPrioridadAscIdAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    /** El rol o el sector aparece como aprobador en algún paso de una regla activa. */
    @Query("select count(p) > 0 from ReglaPaso p where p.regla.activa = true"
            + " and ((p.tipoAprobador = :tipoRol and p.rol = :rol)"
            + " or (p.tipoAprobador = :tipoSector and p.sectorId = :sector))")
    boolean esAprobador(@Param("rol") Rol rol, @Param("sector") Long sector,
                        @Param("tipoRol") TipoAprobador tipoRol, @Param("tipoSector") TipoAprobador tipoSector);
}
