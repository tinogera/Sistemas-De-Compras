package com.Sistem.Solicitude_Compra.solicitudes.repositorio;

import com.Sistem.Solicitude_Compra.solicitudes.dominio.AdjuntoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdjuntoItemRepository extends JpaRepository<AdjuntoItem, Long> {

    List<AdjuntoItem> findByItemIdOrderBySubidoEnAsc(Long itemId);
}
