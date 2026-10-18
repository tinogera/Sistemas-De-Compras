package com.Sistem.Solicitude_Compra.aprobaciones.dominio;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(schema = "aprobaciones", name = "regla")
public class Regla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(nullable = false)
    private int prioridad;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(nullable = false)
    private boolean porDefecto = false;

    private Long creadaPor;

    // La columna es NOT NULL: Hibernate inserta el valor explícito, no usa el DEFAULT now() de la base.
    @Column(nullable = false)
    private Instant creadaEn = Instant.now();

    private Long modificadaPor;

    private Instant modificadaEn;

    // Sin valor inicial: Hibernate lo setea en 0 al insertar. La columna es INT, por eso Integer y no Long.
    @Version
    @Setter(AccessLevel.NONE)
    private Integer version;

    // Set y no List: Hibernate no permite traer dos "bags" (List) en la misma consulta (EntityGraph).
    @OneToMany(mappedBy = "regla", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private Set<ReglaCondicion> condiciones = new LinkedHashSet<>();

    @OneToMany(mappedBy = "regla", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Setter(AccessLevel.NONE)
    private List<ReglaPaso> pasos = new ArrayList<>();

    public void agregarCondicion(ReglaCondicion condicion) {
        condicion.setRegla(this);
        condiciones.add(condicion);
    }

    public void agregarPaso(ReglaPaso paso) {
        paso.setRegla(this);
        pasos.add(paso);
    }
}