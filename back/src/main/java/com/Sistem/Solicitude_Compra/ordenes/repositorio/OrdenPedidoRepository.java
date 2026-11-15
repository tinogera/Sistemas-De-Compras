package com.Sistem.Solicitude_Compra.ordenes.repositorio;

import com.Sistem.Solicitude_Compra.ordenes.dominio.EstadoOrden;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OrdenPedidoRepository extends JpaRepository<OrdenPedido, Long> {
    boolean existsByItemId(Long itemId);

    Optional<OrdenPedido> findByItemId(Long itemId);

    Page<OrdenPedido> findByEstado(EstadoOrden estado, Pageable pageable);

    @Query(value = "select nextval('ordenes.numero_orden_seq')", nativeQuery = true)
    Long siguienteNumero();

    /** Pasa la orden a ENVIADA solo si sigue GENERADA. Devuelve cuántas filas cambió (0 si ya estaba enviada). */
    default int marcarEnviada(Long id, Long usuarioId, Instant ahora) {
        return cambiarEstado(id, EstadoOrden.GENERADA, EstadoOrden.ENVIADA, usuarioId, ahora);
    }

    @Modifying(clearAutomatically = true)
    @Query("update OrdenPedido o set o.estado = :nuevo, o.enviadaPor = :usuarioId, o.enviadaEn = :ahora "
            + "where o.id = :id and o.estado = :esperado")
    int cambiarEstado(@Param("id") Long id, @Param("esperado") EstadoOrden esperado,
                      @Param("nuevo") EstadoOrden nuevo, @Param("usuarioId") Long usuarioId,
                      @Param("ahora") Instant ahora);
}
