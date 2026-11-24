package com.Sistem.Solicitude_Compra.aprobaciones.repository;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.EventoAprobacionProcesado;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoAprobacionProcesadoRepository extends JpaRepository<EventoAprobacionProcesado, UUID> {
}
