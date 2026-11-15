package com.Sistem.Solicitude_Compra.compras.web;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
@Transactional
class BandejaComprasControllerTest {

    private static final String RUTA = "/api/v1/bandeja";
    private static final Long USUARIO_ID = 77L;
    private static final Long OTRO_ENCARGADO_ID = 88L;
    private static final Long CATEGORIA_ID = 5L;

    @Autowired MockMvc mockMvc;
    @Autowired GestionItemRepository gestiones;

    @MockitoBean UsuarioActual usuarioActual;
    @MockitoBean CatalogoApi catalogoApi;
    @MockitoBean AuthApi authApi;

    @BeforeAll
    static void configurarZonaHoraria() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void prepararDatos() {
        when(usuarioActual.id()).thenReturn(USUARIO_ID);
        when(catalogoApi.nombreCategoria(anyLong())).thenReturn("Papelería");
        when(authApi.obtenerUsuario(USUARIO_ID)).thenReturn(resumen(USUARIO_ID, "Ana Compras"));
        when(authApi.obtenerUsuario(OTRO_ENCARGADO_ID)).thenReturn(resumen(OTRO_ENCARGADO_ID, "Beto Compras"));

        gestiones.deleteAll();
        guardar(1L, Urgencia.MEDIA, "2026-12-10");
        guardar(2L, Urgencia.ALTA, "2026-12-20");
        tomada(guardar(3L, Urgencia.ALTA, "2026-12-05"), USUARIO_ID);
        aprobada(tomada(guardar(4L, Urgencia.BAJA, "2026-12-01"), USUARIO_ID));
        tomada(guardar(5L, Urgencia.MEDIA, "2026-12-01"), OTRO_ENCARGADO_ID);
        guardar(8L, Urgencia.ALTA, "2026-12-05");
        rechazada(guardar(6L, Urgencia.ALTA, "2026-11-01"));
        cancelada(guardar(7L, Urgencia.ALTA, "2026-11-02"));
    }

    @Test
    void ordenaPorPrioridadFechaNecesariaYItemId() throws Exception {
        mockMvc.perform(get(RUTA).with(encargado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].itemId").value(contains(3, 8, 2, 5, 1, 4)))
                .andExpect(jsonPath("$.totalElementos").value(6));
    }

    @Test
    void excluyeLosItemsQueNoEstanPendientesNiEnCotizacion() throws Exception {
        mockMvc.perform(get(RUTA).with(encargado()))
                .andExpect(jsonPath("$.contenido[?(@.itemId == 6)]").isEmpty())
                .andExpect(jsonPath("$.contenido[?(@.itemId == 7)]").isEmpty());
    }

    @Test
    void asignadosAMiTraeSoloLosItemsDelUsuarioLogueado() throws Exception {
        mockMvc.perform(get(RUTA).param("asignadosAMi", "true").with(encargado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].itemId").value(contains(3, 4)))
                .andExpect(jsonPath("$.contenido[*].encargadoId").value(contains(77, 77)));
    }

    @Test
    void asignadosAMiEnFalseNoFiltra() throws Exception {
        mockMvc.perform(get(RUTA).param("asignadosAMi", "false").with(encargado()))
                .andExpect(jsonPath("$.totalElementos").value(6));
    }

    @Test
    void urgenciaAltaTraeSoloItemsDeEsaPrioridad() throws Exception {
        mockMvc.perform(get(RUTA).param("urgencia", "ALTA").with(encargado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].itemId").value(contains(3, 8, 2)))
                .andExpect(jsonPath("$.contenido[*].urgencia").value(contains("ALTA", "ALTA", "ALTA")));
    }

    @Test
    void paraComprarTraeSoloItemsEnCotizacionYaAprobados() throws Exception {
        mockMvc.perform(get(RUTA).param("paraComprar", "true").with(encargado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].itemId").value(contains(4)))
                .andExpect(jsonPath("$.contenido[0].estado").value("EN_COTIZACION"))
                .andExpect(jsonPath("$.contenido[0].estadoAprobacion").value("APROBADA"));
    }

    @Test
    void paginaRespetandoElOrdenDeterministaYElTotal() throws Exception {
        mockMvc.perform(get(RUTA).param("page", "1").param("size", "2").with(encargado()))
                .andExpect(jsonPath("$.contenido[*].itemId").value(contains(2, 5)))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanio").value(2))
                .andExpect(jsonPath("$.totalElementos").value(6))
                .andExpect(jsonPath("$.totalPaginas").value(3));
    }

    @Test
    void completaLosNombresYLosResuelveUnaSolaVezPorPagina() throws Exception {
        mockMvc.perform(get(RUTA).param("asignadosAMi", "true").with(encargado()))
                .andExpect(jsonPath("$.contenido[*].categoriaNombre").value(contains("Papelería", "Papelería")))
                .andExpect(jsonPath("$.contenido[*].encargadoNombre").value(contains("Ana Compras", "Ana Compras")));

        verify(catalogoApi, times(1)).nombreCategoria(CATEGORIA_ID);
        verify(authApi, times(1)).obtenerUsuario(USUARIO_ID);
    }

    @Test
    void itemSinEncargadoDevuelveEncargadoNulo() throws Exception {
        mockMvc.perform(get(RUTA).param("urgencia", "MEDIA").with(encargado()))
                .andExpect(jsonPath("$.contenido[?(@.itemId == 1)].encargadoId").value(contains((Object) null)))
                .andExpect(jsonPath("$.contenido[?(@.itemId == 1)].encargadoNombre").value(contains((Object) null)));
    }

    @Test
    void detalleDevuelveLaGestionConSusNombres() throws Exception {
        mockMvc.perform(get(RUTA + "/3").with(encargado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(3))
                .andExpect(jsonPath("$.solicitudId").value(10))
                .andExpect(jsonPath("$.categoriaNombre").value("Papelería"))
                .andExpect(jsonPath("$.encargadoNombre").value("Ana Compras"))
                .andExpect(jsonPath("$.estado").value("EN_COTIZACION"))
                .andExpect(jsonPath("$.estadoAprobacion").value("EN_CURSO"));
    }

    @Test
    void detalleInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get(RUTA + "/999").with(encargado()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("GestionItem no encontrado"));
    }

    @Test
    void otroRolRecibe403() throws Exception {
        mockMvc.perform(get(RUTA).with(conRol(Rol.SOLICITANTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        mockMvc.perform(get(RUTA + "/3").with(conRol(Rol.SOLICITANTE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinAutenticarRecibe401() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    private static RequestPostProcessor encargado() {
        return conRol(Rol.ENCARGADO);
    }

    private static RequestPostProcessor conRol(Rol rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    private static UsuarioResumen resumen(Long id, String nombre) {
        return new UsuarioResumen(id, nombre, nombre.toLowerCase().replace(' ', '.') + "@test.com",
                Rol.ENCARGADO, 2L, true);
    }

    private GestionItem nuevaGestion(Long itemId, Urgencia urgencia, String fechaNecesaria) {
        return new GestionItem(itemId, 10L, 2L, CATEGORIA_ID, TipoItem.PRODUCTO, "Resma A4 " + itemId,
                new BigDecimal("3"), urgencia, LocalDate.parse(fechaNecesaria), OffsetDateTime.now(),
                new BigDecimal("1500.00"), Moneda.ARS);
    }

    private GestionItem guardar(Long itemId, Urgencia urgencia, String fechaNecesaria) {
        return gestiones.saveAndFlush(nuevaGestion(itemId, urgencia, fechaNecesaria));
    }

    private GestionItem tomada(GestionItem gestion, Long encargadoId) {
        gestion.tomar(encargadoId);
        return gestiones.saveAndFlush(gestion);
    }

    private void aprobada(GestionItem gestion) {
        ReflectionTestUtils.setField(gestion, "estadoAprobacion", EstadoAprobacion.APROBADA);
        gestiones.saveAndFlush(gestion);
    }

    private void rechazada(GestionItem gestion) {
        gestion.rechazar("Sin presupuesto", OffsetDateTime.now());
        gestiones.saveAndFlush(gestion);
    }

    private void cancelada(GestionItem gestion) {
        gestion.cancelar(OffsetDateTime.now());
        gestiones.saveAndFlush(gestion);
    }
}