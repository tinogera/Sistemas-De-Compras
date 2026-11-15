package com.Sistem.Solicitude_Compra.auth.dominio;

import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

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

    private int intentosFallidos;
    private Instant bloqueadoHasta;

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /** Desde el 5.º fallo seguido bloquea 10 minutos; desde el 10.º, 20 minutos. */
    public void registrarFallo(Instant ahora) {
        intentosFallidos++;
        if (intentosFallidos >= 10) {
            bloqueadoHasta = ahora.plus(Duration.ofMinutes(20));
        } else if (intentosFallidos >= 5) {
            bloqueadoHasta = ahora.plus(Duration.ofMinutes(10));
        }
    }

    public void registrarExito() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

}








