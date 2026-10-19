package com.Sistem.Solicitude_Compra.auth;

import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.auth.repositorio.SectorRepository;
import com.Sistem.Solicitude_Compra.auth.repositorio.UsuarioRepository;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuthApi {
    private final UsuarioRepository usuarios;
    private final SectorRepository sectores;

    public AuthApi(UsuarioRepository usuarios, SectorRepository sectores) {
        this.usuarios = usuarios;
        this.sectores = sectores;
    }

    public UsuarioResumen obtenerUsuario(Long id) {
        Usuario usuario = buscarUsuario(id);
        return new UsuarioResumen(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol(),
                usuario.getSector().getId(), Boolean.TRUE.equals(usuario.getActivo()));
    }

    public Long sectorDe(Long usuarioId) {
        return buscarUsuario(usuarioId).getSector().getId();
    }

    public String nombreSector(Long sectorId) {
        return sectores.findById(sectorId)
                .orElseThrow(() -> new NoEncontradoException("No existe el sector " + sectorId))
                .getNombre();
    }

    public boolean existeSector(Long sectorId) {
        return sectores.existsById(sectorId);
    }

    public boolean hayUsuariosActivosConRol(Rol rol) {
        return usuarios.existsByRolAndActivoTrue(rol);
    }

    public boolean hayUsuariosActivosEnSector(Long idSector) {
        return usuarios.existsBySectorIdAndActivoTrue(idSector);
    }

    public List<String> emailsActivosConRol(Rol rol) {
        return usuarios.findByRolAndActivoTrue(rol).stream().map(Usuario::getEmail).toList();
    }

    public List<String> emailsActivosDelSector(Long idSector) {
        return usuarios.findBySectorIdAndActivoTrue(idSector).stream().map(Usuario::getEmail).toList();
    }

    private Usuario buscarUsuario(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el usuario " + id));
    }
}
