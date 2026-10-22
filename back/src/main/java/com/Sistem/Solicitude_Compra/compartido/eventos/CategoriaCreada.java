package com.Sistem.Solicitude_Compra.compartido.eventos;

import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import java.time.Instant;
import java.util.UUID;

public record CategoriaCreada(
        UUID eventoId,
        Instant ocurridoEn,
        Long categoriaId,
        String nombre,
        TipoItem tipo) {
}
