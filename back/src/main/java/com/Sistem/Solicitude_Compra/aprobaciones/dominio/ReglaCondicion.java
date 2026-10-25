package com.Sistem.Solicitude_Compra.aprobaciones.dominio;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.OperadorCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoCondicion;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(schema = "aprobaciones", name = "regla_condicion")
public class ReglaCondicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regla_id", nullable = false)
    private Regla regla;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCondicion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private OperadorCondicion operador;

    // Para MONTO: el importe contra el que se compara (siempre con compareTo, nunca equals).
    @Column(precision = 16, scale = 2)
    private BigDecimal valorNumerico;

    // La columna es CHAR(3); sin @JdbcTypeCode la validación de esquema espera VARCHAR.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 3)
    private Moneda moneda;

    // Reservado para condiciones futuras (categoría, proveedor, etc.).
    private Long valorId;
}