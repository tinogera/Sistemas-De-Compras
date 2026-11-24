package com.Sistem.Solicitude_Compra.aprobaciones.repository;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.AprobacionItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AprobacionItemRepository extends JpaRepository<AprobacionItem, Long> {

    /** Todas las aprobaciones, las más urgentes y antiguas primero (para que el Admin vea las trabadas). */
    @Query(value = """
            select i from AprobacionItem i
            order by case i.urgencia
                when com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia.ALTA then 1
                when com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia.MEDIA then 2
                else 3 end, i.solicitadoEn""",
            countQuery = "select count(i) from AprobacionItem i")
    Page<AprobacionItem> todasPorUrgencia(Pageable pageable);
}
