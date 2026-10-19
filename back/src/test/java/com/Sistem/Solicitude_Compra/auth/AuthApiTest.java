package com.Sistem.Solicitude_Compra.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.auth.dominio.Sector;
import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.auth.repositorio.SectorRepository;
import com.Sistem.Solicitude_Compra.auth.repositorio.UsuarioRepository;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthApiTest {

    @Mock UsuarioRepository usuarios;
    @Mock SectorRepository sectores;
    @InjectMocks AuthApi authApi;

    private Sector sector(Long id, String nombre) {
        Sector sector = new Sector();
        sector.setId(id);
        sector.setNombre(nombre);
        return sector;
    }

    private Usuario usuario(Long id, String email, Boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Nombre " + id);
        usuario.setEmail(email);
        usuario.setRol(Rol.ENCARGADO);
        usuario.setSector(sector(2L, "Compras"));
        usuario.setActivo(activo);
        return usuario;
    }

    @Test
    void obtenerUsuario_devuelveElResumen() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario(1L, "a@test.com", true)));

        UsuarioResumen resumen = authApi.obtenerUsuario(1L);

        assertEquals(new UsuarioResumen(1L, "Nombre 1", "a@test.com", Rol.ENCARGADO, 2L, true), resumen);
    }

    @Test
    void obtenerUsuario_conActivoNull_loMapeaComoInactivo() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario(1L, "a@test.com", null)));

        assertFalse(authApi.obtenerUsuario(1L).activo());
    }

    @Test
    void obtenerUsuario_inexistente_lanzaNoEncontrado() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> authApi.obtenerUsuario(99L));
    }

    @Test
    void sectorDe_devuelveElIdDelSector() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario(1L, "a@test.com", true)));

        assertEquals(2L, authApi.sectorDe(1L));
    }

    @Test
    void sectorDe_usuarioInexistente_lanzaNoEncontrado() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> authApi.sectorDe(99L));
    }

    @Test
    void nombreSector_devuelveElNombre() {
        when(sectores.findById(2L)).thenReturn(Optional.of(sector(2L, "Compras")));

        assertEquals("Compras", authApi.nombreSector(2L));
    }

    @Test
    void nombreSector_inexistente_lanzaNoEncontrado() {
        when(sectores.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> authApi.nombreSector(99L));
    }

    @Test
    void existeSector_devuelveTrueYFalse() {
        when(sectores.existsById(2L)).thenReturn(true);
        when(sectores.existsById(99L)).thenReturn(false);

        assertTrue(authApi.existeSector(2L));
        assertFalse(authApi.existeSector(99L));
    }

    @Test
    void hayUsuariosActivosConRol_delegaEnElRepositorio() {
        when(usuarios.existsByRolAndActivoTrue(Rol.ENCARGADO)).thenReturn(true);
        when(usuarios.existsByRolAndActivoTrue(Rol.ADMIN)).thenReturn(false);

        assertTrue(authApi.hayUsuariosActivosConRol(Rol.ENCARGADO));
        assertFalse(authApi.hayUsuariosActivosConRol(Rol.ADMIN));
    }

    @Test
    void hayUsuariosActivosEnSector_delegaEnElRepositorio() {
        when(usuarios.existsBySectorIdAndActivoTrue(2L)).thenReturn(true);
        when(usuarios.existsBySectorIdAndActivoTrue(99L)).thenReturn(false);

        assertTrue(authApi.hayUsuariosActivosEnSector(2L));
        assertFalse(authApi.hayUsuariosActivosEnSector(99L));
    }

    @Test
    void emailsActivosConRol_devuelveSoloLosEmails() {
        when(usuarios.findByRolAndActivoTrue(Rol.ENCARGADO))
                .thenReturn(List.of(usuario(1L, "a@test.com", true), usuario(2L, "b@test.com", true)));

        assertEquals(List.of("a@test.com", "b@test.com"), authApi.emailsActivosConRol(Rol.ENCARGADO));
    }

    @Test
    void emailsActivosConRol_sinUsuarios_devuelveListaVacia() {
        when(usuarios.findByRolAndActivoTrue(Rol.ADMIN)).thenReturn(List.of());

        assertTrue(authApi.emailsActivosConRol(Rol.ADMIN).isEmpty());
    }

    @Test
    void emailsActivosDelSector_devuelveSoloLosEmails() {
        when(usuarios.findBySectorIdAndActivoTrue(2L))
                .thenReturn(List.of(usuario(1L, "a@test.com", true), usuario(2L, "b@test.com", true)));

        assertEquals(List.of("a@test.com", "b@test.com"), authApi.emailsActivosDelSector(2L));
    }

    @Test
    void emailsActivosDelSector_sinUsuarios_devuelveListaVacia() {
        when(usuarios.findBySectorIdAndActivoTrue(99L)).thenReturn(List.of());

        assertTrue(authApi.emailsActivosDelSector(99L).isEmpty());
    }
}
