package com.Sistem.Solicitude_Compra.auth.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.auth.repositorio.UsuarioRepository;
import com.Sistem.Solicitude_Compra.compartido.errores.CredencialesInvalidasException;
import com.Sistem.Solicitude_Compra.compartido.errores.CuentaBloqueadaException;

class LoginServiceTest {

    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    LoginService service;

    LoginServiceTest() {
        when(encoder.encode(anyString())).thenReturn("hash-relleno");
        service = new LoginService(usuarios, encoder, mock(JwtEncoder.class), 8);
    }

    @Test
    void emailInexistenteIgualCorreBcryptContraElHashDeRelleno() {
        when(usuarios.findByEmailIgnoreCase("nadie@empresa.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> service.login("nadie@empresa.com", "clave"));

        verify(encoder).matches("clave", "hash-relleno");
    }

    @Test
    void usuarioInactivoIgualCorreBcrypt() {
        Usuario inactivo = new Usuario();
        inactivo.setActivo(false);
        inactivo.setPasswordHash("hash-real");
        when(usuarios.findByEmailIgnoreCase("baja@empresa.com")).thenReturn(Optional.of(inactivo));
        when(encoder.matches("clave", "hash-real")).thenReturn(true);

        assertThrows(CredencialesInvalidasException.class, () -> service.login("baja@empresa.com", "clave"));

        verify(encoder).matches("clave", "hash-real");
    }

    @Test
    void laClaveIncorrectaSumaUnIntento() {
        Usuario u = usuarioActivo();
        when(usuarios.findByEmailIgnoreCase("ana@empresa.com")).thenReturn(Optional.of(u));

        assertThrows(CredencialesInvalidasException.class, () -> service.login("ana@empresa.com", "mala"));

        assertEquals(1, u.getIntentosFallidos());
    }

    @Test
    void conLaCuentaBloqueadaRechazaAunqueLaClaveSeaCorrecta() {
        Usuario u = usuarioActivo();
        for (int i = 0; i < 5; i++) {
            u.registrarFallo(java.time.Instant.now());
        }
        when(usuarios.findByEmailIgnoreCase("ana@empresa.com")).thenReturn(Optional.of(u));
        when(encoder.matches("buena", "hash-real")).thenReturn(true);

        assertThrows(CuentaBloqueadaException.class, () -> service.login("ana@empresa.com", "buena"));

        assertTrue(u.getIntentosFallidos() == 5);
    }

    private Usuario usuarioActivo() {
        Usuario u = new Usuario();
        u.setActivo(true);
        u.setPasswordHash("hash-real");
        return u;
    }
}
