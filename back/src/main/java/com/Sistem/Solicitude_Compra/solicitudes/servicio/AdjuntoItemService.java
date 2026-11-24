package com.Sistem.Solicitude_Compra.solicitudes.servicio;

import com.Sistem.Solicitude_Compra.compartido.archivos.AdjuntoResponse;
import com.Sistem.Solicitude_Compra.compartido.archivos.AlmacenamientoArchivos;
import com.Sistem.Solicitude_Compra.compartido.archivos.ArchivoValidado;
import com.Sistem.Solicitude_Compra.compartido.archivos.ValidadorArchivos;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.AdjuntoItem;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Item;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.AdjuntoItemRepository;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

@Service
public class AdjuntoItemService {

    private final ItemRepository items;
    private final AdjuntoItemRepository adjuntos;
    private final ValidadorArchivos validador;
    private final AlmacenamientoArchivos almacenamiento;

    public AdjuntoItemService(ItemRepository items, AdjuntoItemRepository adjuntos, ValidadorArchivos validador,
                              AlmacenamientoArchivos almacenamiento) {
        this.items = items;
        this.adjuntos = adjuntos;
        this.validador = validador;
        this.almacenamiento = almacenamiento;
    }

    /** Solo el dueño de la solicitud, y mientras el ítem no esté en un estado final. */
    @Transactional
    public AdjuntoResponse subir(Long itemId, MultipartFile archivo, Long usuarioId) {
        Item item = buscarItem(itemId);
        if (!item.getSolicitud().getSolicitanteId().equals(usuarioId)) {
            throw new SinPermisoException("Solo quien creó la solicitud puede adjuntar archivos");
        }
        if (item.getEstado().esFinal()) {
            throw new ReglaNegocioException("ITEM_FINALIZADO", "El ítem ya terminó: no se pueden agregar adjuntos");
        }
        ArchivoValidado valido = validador.validar(archivo);
        String clave;
        try (InputStream contenido = archivo.getInputStream()) {
            clave = almacenamiento.guardar(contenido, valido.extension());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
        try {
            return de(adjuntos.saveAndFlush(new AdjuntoItem(itemId, clave, nombreOriginal(archivo),
                    valido.mimeType(), valido.tamanoBytes(), usuarioId)));
        } catch (RuntimeException e) {
            almacenamiento.borrar(clave); // sin fila no tiene que quedar el archivo suelto
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<AdjuntoResponse> listar(Long itemId, Long usuarioId, Rol rol) {
        verificarVisibilidad(buscarItem(itemId), usuarioId, rol);
        return adjuntos.findByItemIdOrderBySubidoEnAsc(itemId).stream().map(AdjuntoItemService::de).toList();
    }

    @Transactional(readOnly = true)
    public AdjuntoItem buscar(Long id, Long usuarioId, Rol rol) {
        AdjuntoItem adjunto = adjuntos.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el adjunto " + id));
        verificarVisibilidad(buscarItem(adjunto.getItemId()), usuarioId, rol);
        return adjunto;
    }

    public org.springframework.core.io.Resource contenido(AdjuntoItem adjunto) {
        return almacenamiento.abrir(adjunto.getClaveObjeto());
    }

    private Item buscarItem(Long itemId) {
        return items.findById(itemId).orElseThrow(() -> new NoEncontradoException("No existe el ítem " + itemId));
    }

    /** Pueden verlos el dueño de la solicitud y los Encargados. */
    private void verificarVisibilidad(Item item, Long usuarioId, Rol rol) {
        if (rol != Rol.ENCARGADO && !item.getSolicitud().getSolicitanteId().equals(usuarioId)) {
            throw new SinPermisoException("No tenés permiso para ver los adjuntos de este ítem");
        }
    }

    private static String nombreOriginal(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            return "adjunto";
        }
        String limpio = java.nio.file.Paths.get(nombre.replace('\\', '/')).getFileName().toString();
        return limpio.length() > 255 ? limpio.substring(limpio.length() - 255) : limpio;
    }

    private static AdjuntoResponse de(AdjuntoItem a) {
        return new AdjuntoResponse(a.getId(), a.getNombre(), a.getMimeType(), a.getTamanoBytes(), a.getSubidoEn());
    }
}
