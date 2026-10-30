package com.Sistem.Solicitude_Compra.compras.repositorio;

import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GestionItemRepository extends JpaRepository<GestionItem, Long> {

    boolean existsByItemId(Long itemId);
}