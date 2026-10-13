package com.Sistem.Solicitude_Compra.auth.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.Sistem.Solicitude_Compra.auth.dominio.Sector;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
        boolean existsByNombreIgnoreCase(String nombre);
}