package com.Sistem.Solicitude_Compra.solicitudes.repositorio;

import java.util.Optional;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByIdAndSolicitudId(Long itemId, Long solicitudId);
}
