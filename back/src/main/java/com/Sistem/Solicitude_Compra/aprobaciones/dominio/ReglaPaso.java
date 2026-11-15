package com.Sistem.Solicitude_Compra.aprobaciones.dominio;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;

import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(schema = "aprobaciones", name = "regla_paso")
public class ReglaPaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regla_id", nullable = false)
    private Regla regla;

    @Column(nullable = false)
    private int orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private TipoAprobador tipoAprobador;

    // Solo si tipoAprobador = ROL (la base lo exige con un CHECK).
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Rol rol;

    // Solo si tipoAprobador = SECTOR.
    private Long sectorId;
}