package com.Sistem.Solicitude_Compra.auditoria.dominio;

import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/** Una línea del historial. Se crea una vez y no se modifica nunca (regla 11 del enunciado). */
@Entity
@Immutable
@Table(schema = "auditoria", name = "historial")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntradaHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private EntidadHistorial entidad;

    @Column(name = "entidad_id", nullable = false, updatable = false)
    private Long entidadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private AccionHistorial accion;

    @Column(name = "estado_anterior", length = 20, updatable = false)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", length = 20, updatable = false)
    private String estadoNuevo;

    /** Null cuando lo hizo el sistema. */
    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String comentario;

    @Column(name = "ocurrido_en", nullable = false, updatable = false)
    private Instant ocurridoEn;

    private EntradaHistorial(EntidadHistorial entidad, Long entidadId, AccionHistorial accion,
                             String estadoAnterior, String estadoNuevo, Long usuarioId, String comentario,
                             Instant ocurridoEn) {
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.accion = accion;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuarioId = usuarioId;
        this.comentario = comentario;
        this.ocurridoEn = ocurridoEn;
    }

    public static EntradaHistorial de(EntidadHistorial entidad, Long entidadId, AccionHistorial accion,
                                      String estadoAnterior, String estadoNuevo, Long usuarioId,
                                      String comentario, Instant ocurridoEn) {
        if (entidad == null || entidadId == null || accion == null || ocurridoEn == null) {
            throw new ReglaNegocioException("HISTORIAL_INCOMPLETO",
                    "El historial necesita entidad, id, acción y fecha");
        }
        return new EntradaHistorial(entidad, entidadId, accion, estadoAnterior, estadoNuevo, usuarioId,
                comentario, ocurridoEn);
    }
}
