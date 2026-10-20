package com.Sistem.Solicitude_Compra.solicitudes.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import java.util.Optional;

@Repository 
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    Page<Solicitud> findBySolicitanteId(Long solicitanteId, Pageable pageable);

    Page<Solicitud> findBySolicitanteIdAndEstado(Long solicitanteId, EstadoSolicitud estado, Pageable pageable);
    
    Page<Solicitud> findByEstado(EstadoSolicitud estado, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    Optional<Solicitud> findConItemsById(Long id);
}
