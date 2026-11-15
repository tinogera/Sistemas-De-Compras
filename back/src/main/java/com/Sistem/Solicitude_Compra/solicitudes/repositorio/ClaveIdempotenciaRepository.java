package com.Sistem.Solicitude_Compra.solicitudes.repositorio;

import com.Sistem.Solicitude_Compra.solicitudes.dominio.ClaveIdempotencia;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaveIdempotenciaRepository extends JpaRepository<ClaveIdempotencia, UUID> {
}
