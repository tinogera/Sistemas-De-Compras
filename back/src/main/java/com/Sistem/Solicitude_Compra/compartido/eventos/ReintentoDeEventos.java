package com.Sistem.Solicitude_Compra.compartido.eventos;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reintenta los eventos que quedaron sin procesar (el listener falló, o la app se cayó antes de terminarlo).
 *
 * <p>Al reiniciar, Modulith ya los vuelve a publicar solo (spring.modulith.events.republish-outstanding-events-on-restart).
 * Esto cubre el otro caso: la app sigue corriendo y un listener falló, por ejemplo porque el evento llegó antes
 * que otro del que depende (CompraActualizada antes que CompraRegistrada). Solo se reintentan los que llevan
 * más de {@code antiguedad} sin completarse, para no pisar a los que todavía se están procesando; igual, todos
 * los listeners toleran recibir el mismo evento dos veces.
 *
 * <p>Se puede apagar con {@code app.eventos.reintento.habilitado=false}. Con varias instancias de la app habría
 * que revisarlo: otra instancia podría estar procesando un evento justo cuando acá se reintenta.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "app.eventos.reintento", name = "habilitado", havingValue = "true",
        matchIfMissing = true)
public class ReintentoDeEventos {

    private static final Logger log = LoggerFactory.getLogger(ReintentoDeEventos.class);

    private final IncompleteEventPublications incompletas;
    private final Duration antiguedad;

    public ReintentoDeEventos(IncompleteEventPublications incompletas,
                              @Value("${app.eventos.reintento.antiguedad:PT2M}") Duration antiguedad) {
        this.incompletas = incompletas;
        this.antiguedad = antiguedad;
    }

    @Scheduled(initialDelayString = "${app.eventos.reintento.cada:PT1M}",
            fixedDelayString = "${app.eventos.reintento.cada:PT1M}")
    void reintentar() {
        try {
            incompletas.resubmitIncompletePublicationsOlderThan(antiguedad);
        } catch (RuntimeException e) {
            // Un fallo acá no tiene que cortar la tarea programada: se vuelve a intentar en el próximo ciclo.
            log.warn("No se pudieron reintentar los eventos pendientes", e);
        }
    }
}
