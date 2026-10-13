package com.Sistem.Solicitude_Compra.auth.dominio;

import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(schema = "auth", name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false) @NotNull @Email
    private String email;
    private String nombre;
    @ManyToOne(optional = false) @JoinColumn(name = "sector_id")
    private Sector sector;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Rol rol;
    @Column(nullable = false)
    private String passwordHash;
    private Boolean activo = true;

}








