package com.Sistem.Solicitude_Compra.auth.servicio;

import com.Sistem.Solicitude_Compra.auth.dominio.Sector;
import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.auth.repositorio.SectorRepository;
import com.Sistem.Solicitude_Compra.auth.repositorio.UsuarioRepository;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Crea sectores y un usuario por rol para desarrollo. No es una migración porque las migraciones también
 * corren en producción. Solo corre con DATOS_PRUEBA=true y si todavía no hay usuarios.
 */
@Component
@ConditionalOnProperty(name = "app.datos-prueba", havingValue = "true")
public class DatosDePrueba implements CommandLineRunner {

    private static final List<String> SECTORES = List.of(
            "Recursos Humanos", "Compras", "Administración", "Sistemas", "Gerencia General");

    private final UsuarioRepository usuarios;
    private final SectorRepository sectores;
    private final PasswordEncoder encoder;
    private final String password;

    public DatosDePrueba(UsuarioRepository usuarios, SectorRepository sectores, PasswordEncoder encoder,
                         @Value("${app.datos-prueba-password}") String password) {
        this.usuarios = usuarios;
        this.sectores = sectores;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarios.count() > 0) {
            return;
        }
        if (password == null || password.length() < 8) {
            throw new IllegalStateException("DATOS_PRUEBA=true necesita DATOS_PRUEBA_PASSWORD de 8 caracteres o más");
        }
        Map<String, Sector> porNombre = new HashMap<>();
        for (String nombre : SECTORES) {
            Sector s = sectores.findAll().stream().filter(x -> x.getNombre().equalsIgnoreCase(nombre))
                    .findFirst().orElseGet(() -> {
                        Sector nuevo = new Sector();
                        nuevo.setNombre(nombre);
                        return sectores.save(nuevo);
                    });
            porNombre.put(nombre, s);
        }
        crear("solicitante@test.com", "Solicitante de prueba", porNombre.get("Recursos Humanos"), Rol.SOLICITANTE);
        crear("encargado@test.com", "Encargado de prueba", porNombre.get("Compras"), Rol.ENCARGADO);
        crear("admin@test.com", "Admin de prueba", porNombre.get("Administración"), Rol.ADMIN);
        crear("supervisor@test.com", "Supervisor de prueba", porNombre.get("Administración"), Rol.SUPERVISOR);
        // Aprobador del paso de sector: es un usuario común de Gerencia General.
        crear("gg@test.com", "Gerencia General de prueba", porNombre.get("Gerencia General"), Rol.SOLICITANTE);
    }

    private void crear(String email, String nombre, Sector sector, Rol rol) {
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setNombre(nombre);
        u.setSector(sector);
        u.setRol(rol);
        u.setPasswordHash(encoder.encode(password));
        usuarios.save(u);
    }
}
