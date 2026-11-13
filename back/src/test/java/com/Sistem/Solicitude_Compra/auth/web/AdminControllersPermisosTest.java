package com.Sistem.Solicitude_Compra.auth.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.auth.dominio.Sector;
import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.auth.servicio.EntradaNoAutenticado;
import com.Sistem.Solicitude_Compra.auth.servicio.ManejadorAccesoDenegado;
import com.Sistem.Solicitude_Compra.auth.servicio.SectorAdminService;
import com.Sistem.Solicitude_Compra.auth.servicio.SeguridadConfig;
import com.Sistem.Solicitude_Compra.auth.servicio.UsuarioAdminService;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.web.ManejadorErrores;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest({AdminSectorController.class, AdminUsuarioController.class})
@Import({SeguridadConfig.class, EntradaNoAutenticado.class, ManejadorAccesoDenegado.class, ManejadorErrores.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class AdminControllersPermisosTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SectorAdminService sectores;
    @MockitoBean UsuarioAdminService usuarios;
    @MockitoBean UsuarioActual usuarioActual;

    static RequestPostProcessor como(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Test
    void soloElAdminListaLosSectores() throws Exception {
        when(sectores.listar()).thenReturn(List.of(sector(1L, "Compras")));

        mockMvc.perform(get("/api/v1/admin/sectores").with(como("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Compras"));
        for (String rol : List.of("SOLICITANTE", "ENCARGADO", "SUPERVISOR")) {
            mockMvc.perform(get("/api/v1/admin/sectores").with(como(rol)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        }
    }

    @Test
    void crearSectorDevuelve201() throws Exception {
        when(sectores.crear("Logística")).thenReturn(sector(7L, "Logística"));

        mockMvc.perform(post("/api/v1/admin/sectores").with(como("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Logística\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void sectorDuplicadoDevuelve409() throws Exception {
        when(sectores.crear("Compras")).thenThrow(new ConflictoException("SECTOR_DUPLICADO", "ya existe"));

        mockMvc.perform(post("/api/v1/admin/sectores").with(como("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Compras\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SECTOR_DUPLICADO"));
    }

    @Test
    void sectorSinNombreDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/sectores/1").with(como("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarUsuariosNoDevuelveElHashYSoloEntraElAdmin() throws Exception {
        Usuario u = usuario();
        when(usuarios.listar(any(), any(), anyInt())).thenReturn(new PageImpl<>(List.of(u)));

        mockMvc.perform(get("/api/v1/admin/usuarios").with(como("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].email").value("ana@test.com"))
                .andExpect(jsonPath("$.contenido[0].passwordHash").doesNotExist());
        mockMvc.perform(get("/api/v1/admin/usuarios").with(como("ENCARGADO")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearUsuarioConClaveCortaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/usuarios").with(como("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"nombre\":\"A\",\"sectorId\":1,\"rol\":\"ADMIN\","
                                + "\"passwordInicial\":\"corta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.passwordInicial").exists());
    }

    @Test
    void crearUsuarioDevuelve201() throws Exception {
        when(usuarios.crear("ana@test.com", "Ana", 2L, Rol.SUPERVISOR, "clave1234")).thenReturn(usuario());

        mockMvc.perform(post("/api/v1/admin/usuarios").with(como("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@test.com\",\"nombre\":\"Ana\",\"sectorId\":2,"
                                + "\"rol\":\"SUPERVISOR\",\"passwordInicial\":\"clave1234\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void cambiarLaClaveDevuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/admin/usuarios/5/password").with(como("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"passwordNueva\":\"otraClave123\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void actualizarUsuarioPasaElAdminActual() throws Exception {
        when(usuarioActual.id()).thenReturn(1L);
        when(usuarios.actualizar(eq(5L), eq(1L), any(), any(), any(), eq(false))).thenReturn(usuario());

        mockMvc.perform(patch("/api/v1/admin/usuarios/5").with(como("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk());
    }

    private static Sector sector(Long id, String nombre) {
        Sector s = new Sector();
        s.setId(id);
        s.setNombre(nombre);
        return s;
    }

    private static Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(5L);
        u.setEmail("ana@test.com");
        u.setNombre("Ana");
        u.setSector(sector(2L, "Compras"));
        u.setRol(Rol.SUPERVISOR);
        u.setPasswordHash("secreto");
        return u;
    }
}
