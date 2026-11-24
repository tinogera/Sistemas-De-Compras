package com.Sistem.Solicitude_Compra.integraciones.servicio;

import com.Sistem.Solicitude_Compra.compartido.web.ErrorApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Cadena de seguridad aparte para {@code /api/v1/integraciones/**}: acá no vale el JWT, vale la API key de n8n
 * en el header {@code X-API-Key}. Y esa clave no sirve en el resto de la API (la cadena general no la conoce).
 */
@Configuration
public class SeguridadIntegracion {

    static final String HEADER = "X-API-Key";

    @Bean
    @Order(1)
    SecurityFilterChain filtrosIntegracion(HttpSecurity http, ObjectMapper mapper,
                                           @Value("${app.n8n.api-key:}") String apiKey) throws Exception {
        http.securityMatcher("/api/v1/integraciones/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new FiltroApiKey(apiKey), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(a -> a.anyRequest().hasRole("INTEGRACION"))
                .exceptionHandling(e -> e.authenticationEntryPoint(noAutenticado(mapper)));
        return http.build();
    }

    private static AuthenticationEntryPoint noAutenticado(ObjectMapper mapper) {
        return (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            mapper.writeValue(response.getWriter(), new ErrorApi("NO_AUTENTICADO", "Falta la API key"));
        };
    }

    static class FiltroApiKey extends OncePerRequestFilter {
        private final byte[] clave;

        FiltroApiKey(String apiKey) {
            this.clave = apiKey.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            String recibida = request.getHeader(HEADER);
            // isEqual compara en tiempo constante: con equals se podría adivinar la clave midiendo tiempos.
            if (clave.length > 0 && recibida != null
                    && MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8))) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        "n8n", null, List.of(new SimpleGrantedAuthority("ROLE_INTEGRACION"))));
            }
            chain.doFilter(request, response);
        }
    }
}
