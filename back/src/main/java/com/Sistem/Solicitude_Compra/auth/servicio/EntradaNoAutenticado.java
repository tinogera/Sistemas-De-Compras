package com.Sistem.Solicitude_Compra.auth.servicio;

import com.Sistem.Solicitude_Compra.compartido.web.ErrorApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class EntradaNoAutenticado implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;

    public EntradaNoAutenticado(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(),
                new ErrorApi("NO_AUTENTICADO", "Tenés que iniciar sesión"));
    }
}
