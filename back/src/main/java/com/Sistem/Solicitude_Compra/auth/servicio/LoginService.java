package com.Sistem.Solicitude_Compra.auth.servicio;

import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.auth.repositorio.UsuarioRepository;
import com.Sistem.Solicitude_Compra.auth.web.LoginResponse;
import com.Sistem.Solicitude_Compra.auth.web.UsuarioLogin;
import com.Sistem.Solicitude_Compra.compartido.errores.CredencialesInvalidasException;
import com.Sistem.Solicitude_Compra.compartido.errores.CuentaBloqueadaException;
import com.Sistem.Solicitude_Compra.compartido.errores.ErrorNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class LoginService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final long expiracionHoras;
    // Hash de relleno: si el email no existe igual se corre BCrypt, para que tarde lo mismo que con un email real.
    private final String hashRelleno;

    public LoginService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtEncoder jwtEncoder,
                        @Value("${app.jwt.expiracion-horas}") long expiracionHoras) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.expiracionHoras = expiracionHoras;
        this.hashRelleno = encoder.encode("relleno");
    }

    // noRollbackFor: el fallo se lanza como excepcion, pero el contador de intentos tiene que quedar guardado.
    @Transactional(noRollbackFor = ErrorNegocioException.class)
    public LoginResponse login(String email, String password) {
        Usuario u = usuarios.findByEmailIgnoreCase(email).orElse(null);
        boolean claveOk = encoder.matches(password, u != null ? u.getPasswordHash() : hashRelleno);
        Instant ahora = Instant.now();
        if (u != null && u.estaBloqueado(ahora)) {
            long minutos = Duration.between(ahora, u.getBloqueadoHasta()).toMinutes() + 1;
            throw new CuentaBloqueadaException(minutos);
        }
        if (u == null || !Boolean.TRUE.equals(u.getActivo()) || !claveOk) {
            if (u != null) {
                u.registrarFallo(ahora);
            }
            throw new CredencialesInvalidasException();
        }
        u.registrarExito();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(u.getId().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(expiracionHoras, ChronoUnit.HOURS))
                .claim("email", u.getEmail())
                .claim("nombre", u.getNombre())
                .claim("rol", u.getRol().name())
                .claim("sectorId", u.getSector().getId())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS384).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new LoginResponse(token,
                new UsuarioLogin(u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.getSector().getId()));
    }
}
