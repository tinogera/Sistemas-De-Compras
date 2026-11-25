package com.Sistem.Solicitude_Compra.reportes.servicio;

import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.reportes.repositorio.HechoResolucionRepository;
import com.Sistem.Solicitude_Compra.reportes.repositorio.TiempoPorUrgencia;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tiempo promedio entre que se crea un ítem y se resuelve (comprado o rechazado), con la espera de aprobaciones. */
@Service
@Transactional(readOnly = true)
public class ReporteTiemposService {

    private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final HechoResolucionRepository resoluciones;

    public ReporteTiemposService(HechoResolucionRepository resoluciones) {
        this.resoluciones = resoluciones;
    }

    public TiemposResolucion tiempos(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ParametroInvalidoException("Debe indicar las fechas desde y hasta.");
        }
        if (desde.isAfter(hasta)) {
            throw new ParametroInvalidoException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        // Días completos en hora argentina: de las 00:00 de "desde" a las 00:00 del día siguiente a "hasta".
        Instant inicio = desde.atStartOfDay(ARGENTINA).toInstant();
        Instant fin = hasta.plusDays(1).atStartOfDay(ARGENTINA).toInstant();

        Map<Urgencia, TiempoPorUrgencia> filas = new EnumMap<>(Urgencia.class);
        resoluciones.tiemposPorUrgencia(inicio, fin)
                .forEach(f -> filas.put(Urgencia.valueOf(f.getUrgencia()), f));

        Map<String, TiemposResolucion.PorUrgencia> porUrgencia = new LinkedHashMap<>();
        long total = 0;
        double suma = 0;
        for (Urgencia u : Urgencia.values()) {
            TiempoPorUrgencia f = filas.get(u);
            long cantidad = f == null ? 0 : f.getCantidad();
            Double promedio = f == null || cantidad == 0 ? null : f.getPromedioHoras();
            if (promedio != null) {
                suma += promedio * cantidad; // se pondera por cantidad
            }
            total += cantidad;
            porUrgencia.put(u.name(), new TiemposResolucion.PorUrgencia(cantidad, redondear(promedio)));
        }
        return new TiemposResolucion(total, total == 0 ? null : redondear(suma / total), porUrgencia);
    }

    private static Double redondear(Double horas) {
        return horas == null ? null : BigDecimal.valueOf(horas).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
