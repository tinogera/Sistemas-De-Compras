package com.Sistem.Solicitude_Compra.auth.servicio;

import com.Sistem.Solicitude_Compra.auth.dominio.Sector;
import com.Sistem.Solicitude_Compra.auth.repositorio.SectorRepository;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SectorAdminService {

    private final SectorRepository sectores;

    public SectorAdminService(SectorRepository sectores) {
        this.sectores = sectores;
    }

    @Transactional(readOnly = true)
    public List<Sector> listar() {
        return sectores.findAll(org.springframework.data.domain.Sort.by("nombre"));
    }

    public Sector crear(String nombre) {
        String limpio = nombre.trim();
        if (sectores.existsByNombreIgnoreCase(limpio)) {
            throw duplicado(limpio);
        }
        Sector s = new Sector();
        s.setNombre(limpio);
        return sectores.save(s);
    }

    public Sector renombrar(Long id, String nombre) {
        Sector s = sectores.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el sector " + id));
        String limpio = nombre.trim();
        // Renombrar a la misma palabra con otras mayusculas es valido: solo choca si el nombre es de otro sector.
        if (!s.getNombre().equalsIgnoreCase(limpio) && sectores.existsByNombreIgnoreCase(limpio)) {
            throw duplicado(limpio);
        }
        s.setNombre(limpio);
        return s;
    }

    private ConflictoException duplicado(String nombre) {
        return new ConflictoException("SECTOR_DUPLICADO", "Ya existe un sector llamado " + nombre);
    }
}
