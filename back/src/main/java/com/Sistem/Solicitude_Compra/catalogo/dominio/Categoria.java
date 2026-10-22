package com.Sistem.Solicitude_Compra.catalogo.dominio;

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
}
