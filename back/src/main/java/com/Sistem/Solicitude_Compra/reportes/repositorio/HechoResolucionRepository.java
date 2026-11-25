package com.Sistem.Solicitude_Compra.reportes.repositorio;

import com.Sistem.Solicitude_Compra.reportes.dominio.HechoResolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface HechoResolucionRepository extends JpaRepository<HechoResolucion, Long> {

    /**
     * Cantidad y promedio de horas de resolución por urgencia, de los ítems resueltos en [desde, hasta).
     * Cuenta comprados y rechazados. Es nativa porque Postgres resta las fechas directamente.
     * Una urgencia sin ítems en el rango no devuelve fila.
     */
    @Query(value = """
            select urgencia as urgencia, count(*) as cantidad,
                   cast(avg(extract(epoch from resuelto_en - creado_en)) / 3600 as double precision)
                       as "promedioHoras"
            from reportes.hecho_resolucion
            where resuelto_en >= :desde and resuelto_en < :hasta
            group by urgencia
            """, nativeQuery = true)
    List<TiempoPorUrgencia> tiempoPorUrgencia(@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
