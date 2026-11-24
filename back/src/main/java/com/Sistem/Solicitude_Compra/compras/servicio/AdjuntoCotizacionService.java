package com.Sistem.Solicitude_Compra.compras.servicio;

import com.Sistem.Solicitude_Compra.compartido.archivos.AdjuntoResponse;
import com.Sistem.Solicitude_Compra.compartido.archivos.AlmacenamientoArchivos;
import com.Sistem.Solicitude_Compra.compartido.archivos.ArchivoValidado;
import com.Sistem.Solicitude_Compra.compartido.archivos.ValidadorArchivos;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compras.dominio.AdjuntoCotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.AdjuntoCotizacionRepository;
import com.Sistem.Solicitude_Compra.compras.repositorio.CotizacionRepository;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.List;

/** Presupuestos adjuntos a las cotizaciones. Los endpoints son solo para Encargados. */
@Service
public class AdjuntoCotizacionService {

    private final CotizacionRepository cotizaciones;
    private final GestionItemRepository gestiones;
    private final AdjuntoCotizacionRepository adjuntos;
    private final ValidadorArchivos validador;
    private final AlmacenamientoArchivos almacenamiento;

    public AdjuntoCotizacionService(CotizacionRepository cotizaciones, GestionItemRepository gestiones,
                                    AdjuntoCotizacionRepository adjuntos, ValidadorArchivos validador,
                                    AlmacenamientoArchivos almacenamiento) {
        this.cotizaciones = cotizaciones;
        this.gestiones = gestiones;
        this.adjuntos = adjuntos;
        this.validador = validador;
        this.almacenamiento = almacenamiento;
    }

    /** Solo el Encargado al que está asignado el ítem, y mientras el ítem no esté resuelto. */
    @Transactional
    public AdjuntoResponse subir(Long cotizacionId, MultipartFile archivo, Long encargadoId) {
        Cotizacion cotizacion = buscarCotizacion(cotizacionId);
        GestionItem gestion = gestiones.findById(cotizacion.getItemId())
                .orElseThrow(() -> new NoEncontradoException("No existe el ítem " + cotizacion.getItemId()));
        if (gestion.getEncargadoId() == null || !gestion.getEncargadoId().equals(encargadoId)) {
            throw new SinPermisoException("Este ítem no está asignado a vos.");
        }
        if (gestion.getEstado() == EstadoGestion.COMPRADO || gestion.getEstado() == EstadoGestion.RECHAZADO) {
            throw new ReglaNegocioException("ITEM_RESUELTO", "El ítem ya está resuelto: no se agregan adjuntos.");
        }
        ArchivoValidado valido = validador.validar(archivo);
        String clave;
        try (InputStream contenido = archivo.getInputStream()) {
            clave = almacenamiento.guardar(contenido, valido.extension());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
        try {
            return de(adjuntos.saveAndFlush(new AdjuntoCotizacion(cotizacionId, clave, nombreOriginal(archivo),
                    valido.mimeType(), valido.tamanoBytes(), encargadoId)));
        } catch (RuntimeException e) {
            almacenamiento.borrar(clave); // sin fila no tiene que quedar el archivo suelto
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<AdjuntoResponse> listar(Long cotizacionId) {
        buscarCotizacion(cotizacionId);
        return adjuntos.findByCotizacionIdOrderBySubidoEnAsc(cotizacionId).stream()
                .map(AdjuntoCotizacionService::de).toList();
    }

    @Transactional(readOnly = true)
    public AdjuntoCotizacion buscar(Long id) {
        return adjuntos.findById(id).orElseThrow(() -> new NoEncontradoException("No existe el adjunto " + id));
    }

    public Resource contenido(AdjuntoCotizacion adjunto) {
        return almacenamiento.abrir(adjunto.getClaveObjeto());
    }

    private Cotizacion buscarCotizacion(Long id) {
        return cotizaciones.findById(id).orElseThrow(() -> new NoEncontradoException("No existe la cotización " + id));
    }

    private static String nombreOriginal(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            return "adjunto";
        }
        String limpio = Paths.get(nombre.replace('\\', '/')).getFileName().toString();
        return limpio.length() > 255 ? limpio.substring(limpio.length() - 255) : limpio;
    }

    private static AdjuntoResponse de(AdjuntoCotizacion a) {
        return new AdjuntoResponse(a.getId(), a.getNombre(), a.getMimeType(), a.getTamanoBytes(), a.getSubidoEn());
    }
}
