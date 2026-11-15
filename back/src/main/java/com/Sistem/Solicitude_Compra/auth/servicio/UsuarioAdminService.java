package com.Sistem.Solicitude_Compra.auth.servicio;

import com.Sistem.Solicitude_Compra.auth.dominio.Sector;
import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.auth.repositorio.SectorRepository;
import com.Sistem.Solicitude_Compra.auth.repositorio.UsuarioRepository;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.eventos.EncargadoDesasignado;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UsuarioAdminService {

    private static final int TAMANIO_PAGINA = 20;

    private final UsuarioRepository usuarios;
    private final SectorRepository sectores;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher eventos;

    public UsuarioAdminService(UsuarioRepository usuarios, SectorRepository sectores, PasswordEncoder encoder,
                               ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.sectores = sectores;
        this.encoder = encoder;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listar(String q, Rol rol, int pagina) {
        Specification<Usuario> spec = (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase() + "%";
                filtros.add(cb.or(cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("email")), patron)));
            }
            if (rol != null) {
                filtros.add(cb.equal(root.get("rol"), rol));
            }
            return cb.and(filtros.toArray(Predicate[]::new));
        };
        return usuarios.findAll(spec, PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA, Sort.by("nombre", "id")));
    }

    public Usuario crear(String email, String nombre, Long sectorId, Rol rol, String passwordInicial) {
        String emailLimpio = email.trim();
        if (usuarios.existsByEmailIgnoreCase(emailLimpio)) {
            throw new ConflictoException("EMAIL_DUPLICADO", "Ya existe un usuario con el email " + emailLimpio);
        }
        Usuario u = new Usuario();
        u.setEmail(emailLimpio);
        u.setNombre(nombre.trim());
        u.setSector(buscarSector(sectorId));
        u.setRol(rol);
        u.setPasswordHash(encoder.encode(passwordInicial));
        return usuarios.save(u);
    }

    /** Los campos en null no se tocan. Quitarle el rol de Encargado a alguien, o desactivarlo, lo desasigna. */
    public Usuario actualizar(Long id, Long adminId, String nombre, Long sectorId, Rol rol, Boolean activo) {
        Usuario u = buscar(id);
        boolean eraEncargadoActivo = u.getRol() == Rol.ENCARGADO && Boolean.TRUE.equals(u.getActivo());

        Rol rolNuevo = rol != null ? rol : u.getRol();
        boolean activoNuevo = activo != null ? activo : Boolean.TRUE.equals(u.getActivo());
        boolean sigueSiendoEncargadoActivo = rolNuevo == Rol.ENCARGADO && activoNuevo;

        if (eraEncargadoActivo && !sigueSiendoEncargadoActivo) {
            if (usuarios.countByRolAndActivoTrue(Rol.ENCARGADO) <= 1) {
                throw new ReglaNegocioException("ULTIMO_ENCARGADO",
                        "Asigná otro Encargado antes de desactivar a este");
            }
            eventos.publishEvent(new EncargadoDesasignado(UUID.randomUUID(), Instant.now(), u.getId(), adminId));
        }

        if (nombre != null) {
            u.setNombre(nombre.trim());
        }
        if (sectorId != null) {
            u.setSector(buscarSector(sectorId));
        }
        u.setRol(rolNuevo);
        u.setActivo(activoNuevo);
        return u;
    }

    public void cambiarPassword(Long id, String passwordNueva) {
        Usuario u = buscar(id);
        u.setPasswordHash(encoder.encode(passwordNueva));
        // Si estaba bloqueado por intentos fallidos, la clave nueva lo destraba.
        u.registrarExito();
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new NoEncontradoException("No existe el usuario " + id));
    }

    private Sector buscarSector(Long sectorId) {
        return sectores.findById(sectorId)
                .orElseThrow(() -> new NoEncontradoException("No existe el sector " + sectorId));
    }
}
