package com.Sistem.Solicitude_Compra.auth.repositorio;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.dominio.Sector;
import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AuthRepositoriesTest {

    @Autowired
    SectorRepository sectorRepository;
    @Autowired
    UsuarioRepository usuarioRepository;

    Sector compras;
    Sector sistemas;

    @BeforeEach
    void crearSectores() {
        compras = sectorRepository.saveAndFlush(sector("Compras"));
        sistemas = sectorRepository.saveAndFlush(sector("Sistemas"));
    }

    @Test
    void existePorNombreSinImportarMayusculas() {
        assertThat(sectorRepository.existsByNombreIgnoreCase("COMPRAS")).isTrue();
        assertThat(sectorRepository.existsByNombreIgnoreCase("compras")).isTrue();
    }

    @Test
    void noExistePorNombreSiNoEstaGuardado() {
        assertThat(sectorRepository.existsByNombreIgnoreCase("Marketing")).isFalse();
    }


    @Test
    void buscaPorEmailSinImportarMayusculas() {
        usuarioRepository.saveAndFlush(usuario("Ana@Empresa.com", compras, Rol.SOLICITANTE, true));

        var encontrado = usuarioRepository.findByEmailIgnoreCase("ANA@empresa.COM");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getEmail()).isEqualTo("Ana@Empresa.com");
    }

    @Test
    void buscaPorEmailInexistenteDevuelveVacio() {
        assertThat(usuarioRepository.findByEmailIgnoreCase("nadie@empresa.com")).isEmpty();
    }

    @Test
    void existePorEmailSinImportarMayusculas() {
        usuarioRepository.saveAndFlush(usuario("ana@empresa.com", compras, Rol.SOLICITANTE, true));

        assertThat(usuarioRepository.existsByEmailIgnoreCase("ANA@EMPRESA.COM")).isTrue();
        assertThat(usuarioRepository.existsByEmailIgnoreCase("otra@empresa.com")).isFalse();
    }


    @Test
    void listaSoloLosUsuariosActivosDeUnRol() {
        usuarioRepository.saveAndFlush(usuario("enc1@empresa.com", compras, Rol.ENCARGADO, true));
        usuarioRepository.saveAndFlush(usuario("enc2@empresa.com", sistemas, Rol.ENCARGADO, true));
        usuarioRepository.saveAndFlush(usuario("enc3@empresa.com", compras, Rol.ENCARGADO, false));
        usuarioRepository.saveAndFlush(usuario("sol@empresa.com", compras, Rol.SOLICITANTE, true));

        var encargados = usuarioRepository.findByRolAndActivoTrue(Rol.ENCARGADO);

        assertThat(encargados).extracting(Usuario::getEmail)
                .containsExactlyInAnyOrder("enc1@empresa.com", "enc2@empresa.com");
    }

    @Test
    void cuentaSoloLosActivosDeUnRol() {
        usuarioRepository.saveAndFlush(usuario("enc1@empresa.com", compras, Rol.ENCARGADO, true));
        usuarioRepository.saveAndFlush(usuario("enc2@empresa.com", compras, Rol.ENCARGADO, false));

        assertThat(usuarioRepository.countByRolAndActivoTrue(Rol.ENCARGADO)).isEqualTo(1);
        assertThat(usuarioRepository.countByRolAndActivoTrue(Rol.SUPERVISOR)).isZero();
    }

    @Test
    void existeUnActivoConEseRolSoloSiHayAlguno() {
        usuarioRepository.saveAndFlush(usuario("sup@empresa.com", compras, Rol.SUPERVISOR, false));
        usuarioRepository.saveAndFlush(usuario("enc@empresa.com", compras, Rol.ENCARGADO, true));

        assertThat(usuarioRepository.existsByRolAndActivoTrue(Rol.ENCARGADO)).isTrue();
        assertThat(usuarioRepository.existsByRolAndActivoTrue(Rol.SUPERVISOR)).isFalse();
    }

    @Test
    void listaSoloLosUsuariosActivosDeUnSector() {
        usuarioRepository.saveAndFlush(usuario("a@empresa.com", compras, Rol.SOLICITANTE, true));
        usuarioRepository.saveAndFlush(usuario("b@empresa.com", compras, Rol.ENCARGADO, false));
        usuarioRepository.saveAndFlush(usuario("c@empresa.com", sistemas, Rol.SOLICITANTE, true));

        var delSector = usuarioRepository.findBySectorIdAndActivoTrue(compras.getId());

        assertThat(delSector).extracting(Usuario::getEmail).containsExactly("a@empresa.com");
    }

    @Test
    void existeUnActivoEnElSectorSoloSiHayAlguno() {
        usuarioRepository.saveAndFlush(usuario("a@empresa.com", compras, Rol.SOLICITANTE, true));
        usuarioRepository.saveAndFlush(usuario("c@empresa.com", sistemas, Rol.SOLICITANTE, false));

        assertThat(usuarioRepository.existsBySectorIdAndActivoTrue(compras.getId())).isTrue();
        assertThat(usuarioRepository.existsBySectorIdAndActivoTrue(sistemas.getId())).isFalse();
    }


    private Sector sector(String nombre) {
        Sector s = new Sector();
        s.setNombre(nombre);
        return s;
    }

    private Usuario usuario(String email, Sector sector, Rol rol, boolean activo) {
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setNombre("Usuario de prueba");
        u.setSector(sector);
        u.setRol(rol);
        u.setPasswordHash("hash-de-prueba");
        u.setActivo(activo);
        return u;
    }
}
