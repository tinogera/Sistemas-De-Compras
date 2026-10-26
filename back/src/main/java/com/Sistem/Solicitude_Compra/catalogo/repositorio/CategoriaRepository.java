package com.Sistem.Solicitude_Compra.catalogo.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.enums.EstadoCategoria;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Para no crear duplicados
    Optional<Categoria> findByTipoAndNombreIgnoreCase(TipoItem tipo, String nombre);

    // Autocompletado: excluye las UNIFICADAS
    List<Categoria> findTop10ByTipoAndNombreContainingIgnoreCaseAndEstadoNot(
            TipoItem tipo, String texto, EstadoCategoria estado);

    List<Categoria> findTop10ByTipoAndNombreContainingIgnoreCaseAndEstadoNot(
            TipoItem tipo, String texto, EstadoCategoria estado, Sort sort);

    // Para que el Encargado vea las Nuevas
    Page<Categoria> findByEstado(EstadoCategoria estado, Pageable pageable);
}