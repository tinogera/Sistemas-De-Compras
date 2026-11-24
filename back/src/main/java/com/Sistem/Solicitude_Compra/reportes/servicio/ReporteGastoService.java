package com.Sistem.Solicitude_Compra.reportes.servicio;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.reportes.dominio.Agrupacion;
import com.Sistem.Solicitude_Compra.reportes.repositorio.FilaGasto;
import com.Sistem.Solicitude_Compra.reportes.repositorio.HechoGastoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Reporte de gasto: lo comprado (hay una orden), no lo aprobado ni lo estimado. Sale solo de las tablas de lectura
 * de Reportes; los nombres se piden a las API de Auth y Catálogo.
 */
@Service
@Transactional(readOnly = true)
public class ReporteGastoService {

    private final HechoGastoRepository gastos;
    private final AuthApi authApi;
    private final CatalogoApi catalogoApi;

    public ReporteGastoService(HechoGastoRepository gastos, AuthApi authApi, CatalogoApi catalogoApi) {
        this.gastos = gastos;
        this.authApi = authApi;
        this.catalogoApi = catalogoApi;
    }

    public List<GastoAgrupado> gasto(Agrupacion agrupar, LocalDate desde, LocalDate hasta) {
        if (agrupar == null) {
            throw new ParametroInvalidoException("Debe indicar por qué agrupar: sector, categoria o proveedor.");
        }
        if (desde == null || hasta == null) {
            throw new ParametroInvalidoException("Debe indicar las fechas desde y hasta.");
        }
        if (desde.isAfter(hasta)) {
            throw new ParametroInvalidoException("La fecha desde no puede ser posterior a la fecha hasta.");
        }

        List<FilaGasto> filas = switch (agrupar) {
            case SECTOR -> gastos.gastoPorSector(desde, hasta);
            case CATEGORIA -> gastos.gastoPorCategoria(desde, hasta);
            case PROVEEDOR -> gastos.gastoPorProveedor(desde, hasta);
        };
        Function<Long, String> nombreDe = switch (agrupar) {
            case SECTOR -> authApi::nombreSector;
            case CATEGORIA -> catalogoApi::nombreCategoria;
            case PROVEEDOR -> id -> catalogoApi.obtenerProveedor(id).razonSocial();
        };

        // Un mismo sector/categoría/proveedor aparece una vez por moneda: se pide el nombre una sola vez.
        Map<Long, String> nombres = new HashMap<>();
        return filas.stream()
                .map(f -> new GastoAgrupado(f.getClave(),
                        nombres.computeIfAbsent(f.getClave(), id -> nombreSeguro(nombreDe, id)),
                        f.getMoneda(), f.getTotal(), f.getEnvio(), f.getCantidad()))
                .sorted(Comparator.comparing(GastoAgrupado::nombre, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(GastoAgrupado::clave)
                        .thenComparing(GastoAgrupado::moneda))
                .toList();
    }

    /** Si el dato ya no existe, el reporte igual sale: el gasto no puede desaparecer por un nombre faltante. */
    private static String nombreSeguro(Function<Long, String> nombreDe, Long id) {
        try {
            return nombreDe.apply(id);
        } catch (NoEncontradoException e) {
            return "Desconocido (" + id + ")";
        }
    }
}
