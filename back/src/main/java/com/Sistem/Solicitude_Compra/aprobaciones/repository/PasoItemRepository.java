package com.Sistem.Solicitude_Compra.aprobaciones.repository;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoCadena;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.EstadoPaso;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.PasoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasoItemRepository extends JpaRepository<PasoItem, Long> {

    String ORDEN_URGENCIA = """
            order by case i.urgencia
                when com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia.ALTA then 1
                when com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia.MEDIA then 2
                else 3 end, i.solicitadoEn""";

    // El paso es del usuario si coincide su rol (paso de ROL) o su sector (paso de SECTOR).
    String ES_MIO = """
            ((p.tipoAprobador = :tipoRol and p.rol = :rol) or (p.tipoAprobador = :tipoSector and p.sectorId = :sector))""";

    // Pasos activos: de los PENDIENTES del ítem, los de menor orden.
    String MENOR_ORDEN_PENDIENTE = """
            (select min(q.orden) from PasoItem q where q.item = i and q.estado = :pendiente)""";

    /** Los pasos activos que el usuario puede decidir y que no son de un pedido suyo. */
    @EntityGraph(attributePaths = "item")
    @Query(value = "select p from PasoItem p join p.item i where i.estado = :curso and p.estado = :pendiente"
            + " and i.solicitanteId <> :usuarioId and " + ES_MIO + " and p.orden = " + MENOR_ORDEN_PENDIENTE
            + " " + ORDEN_URGENCIA,
            countQuery = "select count(p) from PasoItem p join p.item i where i.estado = :curso"
                    + " and p.estado = :pendiente and i.solicitanteId <> :usuarioId and " + ES_MIO
                    + " and p.orden = " + MENOR_ORDEN_PENDIENTE)
    Page<PasoItem> paraDecidir(@Param("usuarioId") Long usuarioId, @Param("rol") Rol rol,
                               @Param("sector") Long sector, @Param("tipoRol") TipoAprobador tipoRol,
                               @Param("tipoSector") TipoAprobador tipoSector, @Param("curso") EstadoCadena curso,
                               @Param("pendiente") EstadoPaso pendiente, Pageable pageable);

    /** Pasos del usuario que todavía no le tocan, en ítems que siguen en curso (solo lectura). */
    @EntityGraph(attributePaths = "item")
    @Query(value = "select p from PasoItem p join p.item i where i.estado = :curso and p.estado = :pendiente"
            + " and " + ES_MIO + " and p.orden > " + MENOR_ORDEN_PENDIENTE + " " + ORDEN_URGENCIA,
            countQuery = "select count(p) from PasoItem p join p.item i where i.estado = :curso"
                    + " and p.estado = :pendiente and " + ES_MIO + " and p.orden > " + MENOR_ORDEN_PENDIENTE)
    Page<PasoItem> enCurso(@Param("rol") Rol rol, @Param("sector") Long sector,
                           @Param("tipoRol") TipoAprobador tipoRol, @Param("tipoSector") TipoAprobador tipoSector,
                           @Param("curso") EstadoCadena curso, @Param("pendiente") EstadoPaso pendiente,
                           Pageable pageable);

    @EntityGraph(attributePaths = "item")
    @Query(value = "select p from PasoItem p where p.decididoPor = :usuarioId order by p.decididoEn desc",
            countQuery = "select count(p) from PasoItem p where p.decididoPor = :usuarioId")
    Page<PasoItem> decididasPor(@Param("usuarioId") Long usuarioId, Pageable pageable);
}
