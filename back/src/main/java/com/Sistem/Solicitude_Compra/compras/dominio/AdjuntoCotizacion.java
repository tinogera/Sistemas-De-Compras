package com.Sistem.Solicitude_Compra.compras.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Presupuesto del proveedor adjunto a una cotización. El contenido está en disco; acá solo los datos. */
@Entity
@Table(schema = "compras", name = "adjunto_cotizacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdjuntoCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cotizacion_id", nullable = false)
    private Long cotizacionId;

    @Column(name = "clave_objeto", nullable = false, length = 500)
    private String claveObjeto;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "subido_por", nullable = false)
    private Long subidoPor;

    @Column(name = "subido_en", nullable = false)
    private Instant subidoEn;

    public AdjuntoCotizacion(Long cotizacionId, String claveObjeto, String nombre, String mimeType,
                             long tamanoBytes, Long subidoPor) {
        this.cotizacionId = cotizacionId;
        this.claveObjeto = claveObjeto;
        this.nombre = nombre;
        this.mimeType = mimeType;
        this.tamanoBytes = tamanoBytes;
        this.subidoPor = subidoPor;
        this.subidoEn = Instant.now();
    }
}
