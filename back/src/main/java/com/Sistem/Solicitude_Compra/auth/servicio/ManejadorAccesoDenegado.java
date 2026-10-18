package com.Sistem.Solicitude_Compra.auth.servicio;

import com.Sistem.Solicitude_Compra.compartido.web.ErrorApi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    private final ObjectMapper mapper;

    public ManejadorAccesoDenegado(ObjectMapper mappers) {
        this.mapper = mappers;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(),
                new ErrorApi("SIN_PERMISO", "No tenés permiso para hacer esto"));
    }
}