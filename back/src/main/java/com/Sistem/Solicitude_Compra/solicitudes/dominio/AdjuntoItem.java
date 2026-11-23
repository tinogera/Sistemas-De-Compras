package com.Sistem.Solicitude_Compra.solicitudes.dominio;

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

/** Archivo adjunto a un ítem (especificación). El contenido está en disco; acá solo los datos. */
@Entity
@Table(schema = "solicitudes", name = "adjunto_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdjuntoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

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

    public AdjuntoItem(Long itemId, String claveObjeto, String nombre, String mimeType, long tamanoBytes,
                       Long subidoPor) {
        this.itemId = itemId;
        this.claveObjeto = claveObjeto;
        this.nombre = nombre;
        this.mimeType = mimeType;
        this.tamanoBytes = tamanoBytes;
        this.subidoPor = subidoPor;
        this.subidoEn = Instant.now();
    }
}
