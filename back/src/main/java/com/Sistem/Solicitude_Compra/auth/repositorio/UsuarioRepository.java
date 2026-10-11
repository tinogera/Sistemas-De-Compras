package com.Sistem.Solicitude_Compra.auth.repositorio;

import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    Optional<Usuario> findByEmailIgnoreCase(String mail);
    List<Usuario> findByRolAndActivoTrue(Rol rol);
    List<Usuario> findBySectorIdAndActivoTrue(Long idActivo);
    long countByRolAndActivoTrue(Rol cantRolActivo);
    boolean existsByRolAndActivoTrue(Rol rolesActivos);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsBySectorIdAndActivoTrue(Long idSector);
}

