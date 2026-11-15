package com.Sistem.Solicitude_Compra.compras.repositorio;

import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GestionItemRepository
        extends JpaRepository<GestionItem, Long>, JpaSpecificationExecutor<GestionItem> {

    boolean existsByItemId(Long itemId);

    List<GestionItem> findByEncargadoIdAndEstado(Long encargadoId, EstadoGestion estado);
}