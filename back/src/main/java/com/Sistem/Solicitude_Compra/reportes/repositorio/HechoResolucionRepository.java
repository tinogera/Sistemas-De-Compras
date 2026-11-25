package com.Sistem.Solicitude_Compra.reportes.repositorio;

import com.Sistem.Solicitude_Compra.reportes.dominio.HechoResolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.time.Instant;

/** Tiempo de resolución (T16.4): se filtra por cuándo se resolvió el ítem, en [desde, hasta). */
public interface HechoResolucionRepository extends JpaRepository<HechoResolucion, Long> {

    @Query(value = """
            SELECT urgencia AS urgencia, count(*) AS cantidad,
                   avg(extract(epoch FROM resuelto_en - creado_en)) / 3600 AS promedioHoras
            FROM reportes.hecho_resolucion
            WHERE resuelto_en >= :desde AND resuelto_en < :hasta
            GROUP BY urgencia
            """, nativeQuery = true)
    List<TiempoPorUrgencia> tiemposPorUrgencia(@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
