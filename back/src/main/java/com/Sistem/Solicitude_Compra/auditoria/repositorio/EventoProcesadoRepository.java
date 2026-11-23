package com.Sistem.Solicitude_Compra.auditoria.repositorio;

import com.Sistem.Solicitude_Compra.auditoria.dominio.EventoProcesado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoProcesadoRepository extends JpaRepository<EventoProcesado, UUID> {
}
