package com.Sistem.Solicitude_Compra.auth.web;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.auth.servicio.LoginService;
import com.Sistem.Solicitude_Compra.compartido.errores.SesionInvalidaException;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginService loginService;
    private final AuthApi authApi;
    private final UsuarioActual usuarioActual;

    public AuthController(LoginService loginService, AuthApi authApi, UsuarioActual usuarioActual) {
        this.loginService = loginService;
        this.authApi = authApi;
        this.usuarioActual = usuarioActual;
    }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request.email(), request.password());
    }

    @GetMapping("/identificate")
    public UsuarioActualResponse identificate() {
        UsuarioResumen usuario = authApi.obtenerUsuario(usuarioActual.id());
        if (!usuario.activo()) {
            throw new SesionInvalidaException();
        }
        return new UsuarioActualResponse(usuario.id(), usuario.nombre(), usuario.email(),
                usuario.rol(), usuario.sectorId(), authApi.nombreSector(usuario.sectorId()));
    }
}
