package com.Sistem.Solicitude_Compra.reportes.dominio;

import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una fila de gasto por orden generada. El id es el de la orden (viene del evento, no se autogenera), así que
 * implementa Persistable: sin eso Spring Data haría merge (SELECT + UPDATE) y la clave primaria no
 * frenaría una segunda inserción del mismo evento.
 */
@Entity
@Table(schema = "reportes", name = "hecho_gasto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HechoGasto implements Persistable<Long> {

    @Id
    @Column(name = "orden_id")
    private Long ordenId;

    @Column(name = "item_id", nullable = false, unique = true)
    private Long itemId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private Moneda moneda;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal importe;

    @Column(name = "monto_envio", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoEnvio = BigDecimal.ZERO;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean nuevo = true;

    public HechoGasto(Long ordenId, Long itemId, LocalDate fecha, Long sectorId, Long categoriaId,
                      Long proveedorId, Moneda moneda, BigDecimal importe) {
        if (ordenId == null || itemId == null || fecha == null || sectorId == null || categoriaId == null
                || proveedorId == null || moneda == null) {
            throw new ReglaNegocioException("HECHO_GASTO_INCOMPLETO", "Faltan datos para registrar el gasto");
        }
        this.ordenId = ordenId;
        this.itemId = itemId;
        this.fecha = fecha;
        this.sectorId = sectorId;
        this.categoriaId = categoriaId;
        this.proveedorId = proveedorId;
        this.moneda = moneda;
        this.importe = validarImporte(importe);
    }

    /** Corrige los montos cuando el Encargado edita la compra. El envío vacío cuenta como 0. */
    public void actualizarMontos(BigDecimal importe, BigDecimal montoEnvio) {
        this.importe = validarImporte(importe);
        if (montoEnvio != null && montoEnvio.signum() < 0) {
            throw new ReglaNegocioException("MONTO_ENVIO_INVALIDO", "El monto de envío no puede ser negativo");
        }
        this.montoEnvio = montoEnvio == null ? BigDecimal.ZERO : montoEnvio;
    }

    private static BigDecimal validarImporte(BigDecimal importe) {
        if (importe == null || importe.signum() < 0) {
            throw new ReglaNegocioException("IMPORTE_INVALIDO", "El importe no puede ser nulo ni negativo");
        }
        return importe;
    }

    @Override
    public Long getId() {
        return ordenId;
    }

    @Override
    public boolean isNew() {
        return nuevo;
    }

    @PostLoad
    @PostPersist
    void marcarComoExistente() {
        nuevo = false;
    }
}
