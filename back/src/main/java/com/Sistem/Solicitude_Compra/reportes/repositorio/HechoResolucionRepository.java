package com.Sistem.Solicitude_Compra.reportes.repositorio;

import com.Sistem.Solicitude_Compra.reportes.dominio.HechoResolucion;
import org.springframework.data.jpa.repository.JpaRepository;

/** Los promedios por urgencia y rango se agregan en T16.4. */
public interface HechoResolucionRepository extends JpaRepository<HechoResolucion, Long> {
}
