package com.Sistem.Solicitude_Compra.auth.servicio;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
