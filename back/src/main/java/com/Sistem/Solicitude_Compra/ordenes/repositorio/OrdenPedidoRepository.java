package com.Sistem.Solicitude_Compra.ordenes.repositorio;

import com.Sistem.Solicitude_Compra.ordenes.dominio.EstadoOrden;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdenPedidoRepository extends JpaRepository<OrdenPedido, Long> {
    boolean existsByItemId(Long itemId);

    Page<OrdenPedido> findByEstado(EstadoOrden estado, Pageable pageable);

    @Query(value = "select nextval('ordenes.numero_orden_seq')", nativeQuery = true)
    Long siguienteNumero();
}
