package com.Sistem.Solicitude_Compra.auth.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.dominio.Sector;
import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.auth.repositorio.SectorRepository;
import com.Sistem.Solicitude_Compra.auth.repositorio.UsuarioRepository;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthControllerTest {

    private static final String EMAIL = "encargado@test.com";
    private static final String CLAVE = "clave12345";

    @Autowired MockMvc mockMvc;
    @Autowired SectorRepository sectores;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtEncoder jwtEncoder;

    private Usuario usuario;

    @BeforeEach
    void crearUsuario() {
        Sector sector = new Sector();
        sector.setNombre("Compras");
        sectores.save(sector);

        usuario = new Usuario();
        usuario.setEmail(EMAIL);
        usuario.setNombre("Encargado");
        usuario.setSector(sector);
        usuario.setRol(Rol.ENCARGADO);
        usuario.setPasswordHash(encoder.encode(CLAVE));
        usuarios.save(usuario);
    }

    private String cuerpoLogin(String email, String clave) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + clave + "\"}";
    }

    private MvcResult login(String clave) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(EMAIL, clave)))
                .andReturn();
    }

    @Test
    void loginCorrectoDevuelveToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(EMAIL, CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value(EMAIL))
                .andExpect(jsonPath("$.usuario.rol").value("ENCARGADO"));
    }

    @Test
    void loginEsCaseInsensitiveEnElEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("ENCARGADO@Test.com", CLAVE)))
                .andExpect(status().isOk());
    }

    @Test
    void passwordIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(EMAIL, "otra-clave")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void usuarioInexistenteDevuelveElMismoError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("nadie@test.com", CLAVE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void usuarioInactivoNoPuedeIniciarSesion() throws Exception {
        usuario.setActivo(false);
        usuarios.save(usuario);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(EMAIL, CLAVE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void loginSinCamposDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void endpointProtegidoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/algo-protegido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void tokenVencidoDevuelve401() throws Exception {
        Instant hace = Instant.now().minus(2, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getId().toString())
                .issuedAt(hace.minus(1, ChronoUnit.HOURS))
                .expiresAt(hace)
                .claim("rol", "ENCARGADO")
                .build();
        String vencido = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS384).build(), claims)).getTokenValue();

        mockMvc.perform(get("/api/v1/algo-protegido").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void tokenConFirmaAlteradaDevuelve401() throws Exception {
        String respuesta = login(CLAVE).getResponse().getContentAsString();
        String token = respuesta.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        // Se cambia un carácter del medio de la firma (la tercera parte), no el último.
        String[] partes = token.split("\\.");
        char[] firma = partes[2].toCharArray();
        int medio = firma.length / 2;
        firma[medio] = firma[medio] == 'A' ? 'B' : 'A';
        String alterado = partes[0] + "." + partes[1] + "." + new String(firma);

        mockMvc.perform(get("/api/v1/algo-protegido").header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void tokenValidoPasaElFiltroYLlegaAlControllerSinPermitirRutaInexistente() throws Exception {
        String respuesta = login(CLAVE).getResponse().getContentAsString();
        String token = respuesta.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        // Autenticado: ya no es 401. La ruta no existe, así que es 404 con el formato común.
        mockMvc.perform(get("/api/v1/algo-protegido").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }
}
