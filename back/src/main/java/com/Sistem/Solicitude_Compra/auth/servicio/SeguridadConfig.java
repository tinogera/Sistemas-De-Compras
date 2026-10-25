package com.Sistem.Solicitude_Compra.auth.servicio;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class SeguridadConfig {

    private static final int BYTES_MINIMOS_CLAVE = 48;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey claveJwt(@Value("${app.jwt.secret}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < BYTES_MINIMOS_CLAVE) {
            throw new IllegalStateException("JWT_SECRET tiene que tener al menos " + BYTES_MINIMOS_CLAVE + " caracteres");
        }
        return new SecretKeySpec(bytes, "HmacSHA384");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey clave) {
        return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS384).build();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey clave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(){
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("rol");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, JwtAuthenticationConverter converter,
                                EntradaNoAutenticado entrada, ManejadorAccesoDenegado denegado,
                                @Value("${springdoc.swagger-ui.enabled:false}") boolean docsHabilitadas)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> {
                    a.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll();
                    // Swagger solo se abre sin login cuando esta habilitado (perfil dev).
                    if (docsHabilitadas) {
                        a.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();
                    }
                    a.requestMatchers("/actuator/health").permitAll();
                    a.anyRequest().authenticated();
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entrada)
                        .accessDeniedHandler(denegado))
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(entrada)
                        .accessDeniedHandler(denegado));
        return http.build();
    }
}
