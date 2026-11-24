package com.Sistem.Solicitude_Compra.reportes.servicio;

import com.Sistem.Solicitude_Compra.compartido.eventos.CategoriaUnificada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenGenerada;
import com.Sistem.Solicitude_Compra.reportes.dominio.HechoGasto;
import com.Sistem.Solicitude_Compra.reportes.dominio.HechoResolucion;
import com.Sistem.Solicitude_Compra.reportes.dominio.ResultadoResolucion;
import com.Sistem.Solicitude_Compra.reportes.repositorio.HechoGastoRepository;
import com.Sistem.Solicitude_Compra.reportes.repositorio.HechoResolucionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

/**
 * Llena el modelo de lectura de Reportes escuchando eventos de los otros módulos.
 * Todos los listeners son idempotentes: el mismo evento dos veces deja el mismo resultado.
 */
@Service
public class AlimentadorDeReportes {

    private static final Logger log = LoggerFactory.getLogger(AlimentadorDeReportes.class);

    /** El "día de emisión" de una orden es el día en la Argentina, no el día UTC. */
    static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final HechoGastoRepository gastos;
    private final HechoResolucionRepository resoluciones;

    public AlimentadorDeReportes(HechoGastoRepository gastos, HechoResolucionRepository resoluciones) {
        this.gastos = gastos;
        this.resoluciones = resoluciones;
    }

    /** Una fila de gasto por orden. Idempotente por ordenId (y por itemId, que también es único). */
    @ApplicationModuleListener
    void alGenerarseLaOrden(OrdenGenerada orden) {
        if (gastos.existsById(orden.ordenId()) || gastos.existsByItemId(orden.itemId())) {
            return;
        }
        gastos.save(new HechoGasto(orden.ordenId(), orden.itemId(), orden.ocurridoEn().atZone(ZONA).toLocalDate(),
                orden.sectorId(), orden.categoriaId(), orden.proveedorId(), orden.moneda(), orden.precioTotal()));
    }

    /**
     * Corrige importe y envío. Aplicar los mismos valores dos veces no cambia nada. Si todavía no existe el
     * hecho (el evento llegó antes que OrdenGenerada), lanza una excepción para que se reintente (T12.1).
     */
    @ApplicationModuleListener
    void alActualizarseLaCompra(CompraActualizada compra) {
        HechoGasto hecho = gastos.findByItemId(compra.itemId())
                .orElseThrow(() -> new IllegalStateException(
                        "Todavía no existe el hecho de gasto del ítem " + compra.itemId()));
        hecho.actualizarMontos(compra.precioTotal(), compra.montoEnvio());
        gastos.save(hecho);
    }

    /** Una fila de resolución por ítem. Idempotente por itemId. */
    @ApplicationModuleListener
    void alResolverseElItem(ItemResuelto item) {
        if (resoluciones.existsById(item.itemId())) {
            return;
        }
        resoluciones.save(new HechoResolucion(item.itemId(), item.urgencia(), item.solicitadoEn(),
                item.resueltoEn(), ResultadoResolucion.valueOf(item.resultado())));
    }

    /** Caso borde 9: los reportes siguen la redirección de la categoría unificada. */
    @ApplicationModuleListener
    void alUnificarseUnaCategoria(CategoriaUnificada unificacion) {
        if (unificacion.origenId().equals(unificacion.destinoId())) {
            return;
        }
        int cambiadas = gastos.reasignarCategoria(unificacion.origenId(), unificacion.destinoId());
        log.info("Categoría {} unificada en {}: {} hechos de gasto reasignados", unificacion.origenId(),
                unificacion.destinoId(), cambiadas);
    }
}
