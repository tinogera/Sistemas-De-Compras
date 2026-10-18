package com.Sistem.Solicitude_Compra.aprobaciones.repository;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Regla;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReglaRepository extends JpaRepository<Regla, Long> {

    /**
     * Reglas activas por prioridad ascendente (la de menor número se evalúa primero).
     * El EntityGraph trae condiciones y pasos en una sola consulta, sin N+1.
     */
    @EntityGraph(attributePaths = {"condiciones", "pasos"})
    List<Regla> findByActivaTrueOrderByPrioridadAsc();
}