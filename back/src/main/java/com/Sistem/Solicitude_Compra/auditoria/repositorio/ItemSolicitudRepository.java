package com.Sistem.Solicitude_Compra.auditoria.repositorio;

import com.Sistem.Solicitude_Compra.auditoria.dominio.ItemSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemSolicitudRepository extends JpaRepository<ItemSolicitud, Long> {

    List<ItemSolicitud> findBySolicitudId(Long solicitudId);
}
