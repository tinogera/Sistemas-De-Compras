package com.Sistem.Solicitude_Compra.aprobaciones.web;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.OperadorCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoCondicion;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Cuerpo para crear (POST) o reemplazar (PUT) una regla. {@code version} solo se usa en el PUT. */
public record ReglaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede pasar los 120 caracteres") String nombre,
        @NotNull(message = "La prioridad es obligatoria") Integer prioridad,
        Boolean activa,
        List<@Valid CondicionRequest> condiciones,
        List<@Valid PasoRequest> pasos,
        Integer version) {

    public record CondicionRequest(
            @NotNull(message = "El tipo de condición es obligatorio") TipoCondicion tipo,
            @NotNull(message = "El operador es obligatorio") OperadorCondicion operador,
            BigDecimal valor,
            Moneda moneda) {
    }

    public record PasoRequest(
            @NotNull(message = "El orden es obligatorio") Integer orden,
            @NotNull(message = "El tipo de aprobador es obligatorio") TipoAprobador tipoAprobador,
            Rol rol,
            Long sectorId) {
    }
}
