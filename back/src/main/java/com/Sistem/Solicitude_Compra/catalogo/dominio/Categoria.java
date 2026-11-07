package com.Sistem.Solicitude_Compra.catalogo.dominio;

import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "catalogo", name = "categoria")
@Getter
@Setter
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nombre;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoItem tipo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCategoria estado = EstadoCategoria.NUEVA;

    @ManyToOne
    @JoinColumn(name = "unificada_en")
    private Categoria unificadaEn;

    /** El Encargado confirma una categoría que escribió a mano un solicitante: NUEVA pasa a CONOCIDA. */
    public void confirmar() {
        if (estado != EstadoCategoria.NUEVA) {
            throw new ReglaNegocioException("TRANSICION_INVALIDA",
                    "Solo se puede confirmar una categoría Nueva y esta está " + estado + ".");
        }
        estado = EstadoCategoria.CONOCIDA;
    }

    /** Deja esta categoría como UNIFICADA dentro de {@code destino}. Los ítems viejos no se tocan. */
    public void unificarEn(Categoria destino) {
        if (this == destino || (id != null && id.equals(destino.getId()))) {
            throw new ReglaNegocioException("UNIFICACION_INVALIDA",
                    "No se puede unificar una categoría consigo misma.");
        }
        if (estado == EstadoCategoria.UNIFICADA) {
            throw new ReglaNegocioException("TRANSICION_INVALIDA", "La categoría ya está unificada.");
        }
        if (tipo != destino.getTipo()) {
            throw new ReglaNegocioException("UNIFICACION_INVALIDA",
                    "Solo se pueden unificar categorías del mismo tipo.");
        }
        if (destino.getEstado() == EstadoCategoria.UNIFICADA) {
            throw new ReglaNegocioException("UNIFICACION_INVALIDA",
                    "La categoría destino ya está unificada en otra; elegí la que quedó vigente.");
        }
        estado = EstadoCategoria.UNIFICADA;
        unificadaEn = destino;
    }
}
