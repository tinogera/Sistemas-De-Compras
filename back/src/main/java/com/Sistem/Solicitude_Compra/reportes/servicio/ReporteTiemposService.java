package com.Sistem.Solicitude_Compra.reportes.servicio;

import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.reportes.repositorio.HechoResolucionRepository;
import com.Sistem.Solicitude_Compra.reportes.repositorio.TiempoPorUrgencia;
import com.Sistem.Solicitude_Compra.reportes.servicio.TiemposResolucion.TiempoUrgencia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiempo promedio de resolución, general y por urgencia. Cuenta ítems comprados y rechazados. */
@Service
@Transactional(readOnly = true)
public class ReporteTiemposService {

    private final HechoResolucionRepository resoluciones;

    public ReporteTiemposService(HechoResolucionRepository resoluciones) {
        this.resoluciones = resoluciones;
    }

    /**
     * Filtra por resuelto_en. Las fechas se toman como días completos en hora de Argentina: desde las 00:00 de
     * {@code desde} hasta las 00:00 del día siguiente a {@code hasta}.
     */
    public TiemposResolucion tiempos(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ParametroInvalidoException("Debe indicar las fechas desde y hasta.");
        }
        if (desde.isAfter(hasta)) {
            throw new ParametroInvalidoException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        Instant inicio = desde.atStartOfDay(AlimentadorDeReportes.ZONA).toInstant();
        Instant fin = hasta.plusDays(1).atStartOfDay(AlimentadorDeReportes.ZONA).toInstant();

        Map<Urgencia, TiempoPorUrgencia> filas = new EnumMap<>(Urgencia.class);
        List<TiempoPorUrgencia> encontradas = resoluciones.tiempoPorUrgencia(inicio, fin);
        for (TiempoPorUrgencia fila : encontradas) {
            filas.put(Urgencia.valueOf(fila.getUrgencia()), fila);
        }

        // Siempre salen las tres urgencias, de la más a la menos urgente.
        Map<Urgencia, TiempoUrgencia> porUrgencia = new LinkedHashMap<>();
        long cantidadTotal = 0;
        double sumaHoras = 0;
        for (Urgencia urgencia : new Urgencia[] {Urgencia.ALTA, Urgencia.MEDIA, Urgencia.BAJA}) {
            TiempoPorUrgencia fila = filas.get(urgencia);
            if (fila == null || fila.getCantidad() == 0) {
                porUrgencia.put(urgencia, new TiempoUrgencia(0, null));
                continue;
            }
            cantidadTotal += fila.getCantidad();
            sumaHoras += fila.getPromedioHoras() * fila.getCantidad();
            porUrgencia.put(urgencia, new TiempoUrgencia(fila.getCantidad(), redondear(fila.getPromedioHoras())));
        }

        // Promedio general: ponderado por cantidad, con los promedios sin redondear.
        Double general = cantidadTotal == 0 ? null : redondear(sumaHoras / cantidadTotal);
        return new TiemposResolucion(cantidadTotal, general, porUrgencia);
    }

    private static Double redondear(double horas) {
        return BigDecimal.valueOf(horas).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
