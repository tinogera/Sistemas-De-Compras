package com.Sistem.Solicitude_Compra.reportes.repositorio;

import com.Sistem.Solicitude_Compra.reportes.dominio.EventoProcesadoReportes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoProcesadoReportesRepository extends JpaRepository<EventoProcesadoReportes, UUID> {
}
