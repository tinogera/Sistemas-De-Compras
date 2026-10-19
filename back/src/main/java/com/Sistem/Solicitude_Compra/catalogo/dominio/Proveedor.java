package com.Sistem.Solicitude_Compra.catalogo.dominio;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(schema = "catalogo", name = "proveedor")
@Setter
@Getter
public class Proveedor {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Pattern(regexp = "[0-9]{2}-[0-9]{8}-[0-9]")
    @Column(unique = true, length = 13)
    private String cuit;

    @Size(max = 50)
    @Column(length = 50)
    private String telefono;

    @Email
    @Size(max = 255)
    @Column(length = 255)
    private String email;

    @Size(max = 150)
    @Column(length = 150)
    private String contacto;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToMany
    @JoinTable(
            schema = "catalogo",
            name = "proveedor_categoria",
            joinColumns = @JoinColumn(name = "proveedor_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    private Set<Categoria> categorias = new HashSet<>();
}
