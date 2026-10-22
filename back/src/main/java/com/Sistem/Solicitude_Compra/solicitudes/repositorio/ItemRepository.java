package com.Sistem.Solicitude_Compra.solicitudes.repositorio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.Sistem.Solicitude_Compra.solicitudes.dominio.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByIdAndSolicitudId(Long itemId, Long solicitudId);
}
