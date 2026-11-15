package com.Sistem.Solicitude_Compra.auth.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.auth.dominio.Sector;
import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.auth.repositorio.SectorRepository;
import com.Sistem.Solicitude_Compra.auth.repositorio.UsuarioRepository;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.eventos.EncargadoDesasignado;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioAdminServiceTest {

    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    SectorRepository sectores = mock(SectorRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    UsuarioAdminService service = new UsuarioAdminService(usuarios, sectores, encoder, eventos);

    @Test
    void creaElUsuarioConLaClaveCifrada() {
        Sector sector = sector(2L);
        when(usuarios.existsByEmailIgnoreCase("ana@test.com")).thenReturn(false);
        when(sectores.findById(2L)).thenReturn(Optional.of(sector));
        when(encoder.encode("clave1234")).thenReturn("hash");
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario u = service.crear(" ana@test.com ", "Ana", 2L, Rol.SUPERVISOR, "clave1234");

        assertEquals("ana@test.com", u.getEmail());
        assertEquals("hash", u.getPasswordHash());
        assertEquals(Rol.SUPERVISOR, u.getRol());
    }

    @Test
    void emailRepetidoDa409() {
        when(usuarios.existsByEmailIgnoreCase("ana@test.com")).thenReturn(true);

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> service.crear("ana@test.com", "Ana", 2L, Rol.SOLICITANTE, "clave1234"));

        assertEquals("EMAIL_DUPLICADO", ex.getCodigo());
        verify(usuarios, never()).save(any());
    }

    @Test
    void sectorInexistenteDa404() {
        when(sectores.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> service.crear("ana@test.com", "Ana", 9L, Rol.SOLICITANTE, "clave1234"));
    }

    @Test
    void elUnicoEncargadoNoSePuedeDesactivar() {
        Usuario encargado = usuario(5L, Rol.ENCARGADO, true);
        when(usuarios.findById(5L)).thenReturn(Optional.of(encargado));
        when(usuarios.countByRolAndActivoTrue(Rol.ENCARGADO)).thenReturn(1L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(5L, 1L, null, null, null, false));

        assertEquals("ULTIMO_ENCARGADO", ex.getCodigo());
        assertThat(encargado.getActivo()).isTrue();
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void elUnicoEncargadoNoPuedePerderElRol() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario(5L, Rol.ENCARGADO, true)));
        when(usuarios.countByRolAndActivoTrue(Rol.ENCARGADO)).thenReturn(1L);

        assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(5L, 1L, null, null, Rol.SOLICITANTE, null));
    }

    @Test
    void desactivarAUnEncargadoConOtrosPublicaEncargadoDesasignado() {
        Usuario encargado = usuario(5L, Rol.ENCARGADO, true);
        when(usuarios.findById(5L)).thenReturn(Optional.of(encargado));
        when(usuarios.countByRolAndActivoTrue(Rol.ENCARGADO)).thenReturn(2L);

        service.actualizar(5L, 1L, null, null, null, false);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventos).publishEvent(captor.capture());
        EncargadoDesasignado evento = (EncargadoDesasignado) captor.getValue();
        assertEquals(5L, evento.usuarioId());
        assertEquals(1L, evento.adminId());
        assertThat(encargado.getActivo()).isFalse();
    }

    @Test
    void cambiarElNombreDeUnEncargadoNoPublicaNada() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario(5L, Rol.ENCARGADO, true)));

        service.actualizar(5L, 1L, "Nuevo", null, null, null);

        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void reactivarUnEncargadoNoPublicaNada() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario(5L, Rol.ENCARGADO, false)));

        service.actualizar(5L, 1L, null, null, null, true);

        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void cambiarLaClaveLaCifraYDestrabaLaCuenta() {
        Usuario u = usuario(5L, Rol.SOLICITANTE, true);
        for (int i = 0; i < 5; i++) {
            u.registrarFallo(Instant.now());
        }
        when(usuarios.findById(5L)).thenReturn(Optional.of(u));
        when(encoder.encode(anyString())).thenReturn("hash-nuevo");

        service.cambiarPassword(5L, "otraClave123");

        assertEquals("hash-nuevo", u.getPasswordHash());
        assertThat(u.estaBloqueado(Instant.now())).isFalse();
    }

    private static Sector sector(Long id) {
        Sector s = new Sector();
        s.setId(id);
        s.setNombre("Compras");
        return s;
    }

    private static Usuario usuario(Long id, Rol rol, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("u" + id + "@test.com");
        u.setNombre("U" + id);
        u.setSector(sector(2L));
        u.setRol(rol);
        u.setActivo(activo);
        u.setPasswordHash("hash");
        return u;
    }
}
