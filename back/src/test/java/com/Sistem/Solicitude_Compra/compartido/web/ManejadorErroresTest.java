package com.Sistem.Solicitude_Compra.compartido.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = ManejadorErroresTest.ControladorPrueba.class)
@Import(ManejadorErroresTest.ControladorPrueba.class)
class ManejadorErroresTest {

    @Autowired
    MockMvc mockMvc;

    record CuerpoPrueba(@NotBlank String nombre, Integer cantidad) {
    }

    @RestController
    @RequestMapping("/prueba")
    static class ControladorPrueba {

        @GetMapping("/no-encontrado")
        void noEncontrado() {
            throw new NoEncontradoException("No existe la solicitud");
        }

        @GetMapping("/regla-negocio")
        void reglaNegocio() {
            throw new ReglaNegocioException("COTIZACION_VENCIDA", "La cotización está vencida");
        }

        @GetMapping("/conflicto")
        void conflicto() {
            throw new ConflictoException("ITEM_TOMADO", "El ítem ya fue tomado");
        }

        @GetMapping("/sin-permiso")
        void sinPermiso() {
            throw new SinPermisoException("No podés ver esta solicitud");
        }

        @GetMapping("/version")
        void version() {
            throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
        }

        @GetMapping("/duplicado")
        void duplicado() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
        }

        @GetMapping("/interno")
        void interno() {
            throw new IllegalStateException("detalle interno que no tiene que salir");
        }

        @PostMapping("/validacion")
        void validacion(@Valid @RequestBody CuerpoPrueba cuerpo) {
        }

        @GetMapping("/parametro")
        void parametro(@RequestParam int numero) {
        }

        @GetMapping("/parametro-validado")
        void parametroValidado(@RequestParam @Min(1) int numero) {
        }
    }

    @Test
    void noEncontradoDevuelve404() throws Exception {
        mockMvc.perform(get("/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("No existe la solicitud"))
                .andExpect(jsonPath("$.campos").doesNotExist());
    }

    @Test
    void reglaNegocioDevuelve422ConSuCodigo() throws Exception {
        mockMvc.perform(get("/prueba/regla-negocio"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("COTIZACION_VENCIDA"));
    }

    @Test
    void conflictoDevuelve409ConSuCodigo() throws Exception {
        mockMvc.perform(get("/prueba/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ITEM_TOMADO"));
    }

    @Test
    void sinPermisoDevuelve403() throws Exception {
        mockMvc.perform(get("/prueba/sin-permiso"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    void choqueDeVersionDevuelve409() throws Exception {
        mockMvc.perform(get("/prueba/version"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"))
                .andExpect(jsonPath("$.mensaje").value("Otro usuario modificó este dato. Recargá la página."));
    }

    @Test
    void duplicadoDevuelve409() throws Exception {
        mockMvc.perform(get("/prueba/duplicado"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICADO"));
    }

    @Test
    void errorInesperadoDevuelve500SinDetalleInterno() throws Exception {
        mockMvc.perform(get("/prueba/interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado."));
    }

    @Test
    void validacionDevuelve400ConCampos() throws Exception {
        mockMvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campos.nombre").exists());
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CUERPO_INVALIDO"));
    }

    @Test
    void tipoIncorrectoEnElCuerpoDevuelve400() throws Exception {
        mockMvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"x\", \"cantidad\": \"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CUERPO_INVALIDO"));
    }

    @Test
    void parametroFaltanteDevuelve400() throws Exception {
        mockMvc.perform(get("/prueba/parametro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }

    @Test
    void parametroConTipoIncorrectoDevuelve400() throws Exception {
        mockMvc.perform(get("/prueba/parametro").param("numero", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }

    @Test
    void parametroQueNoCumpleLaValidacionDevuelve400() throws Exception {
        mockMvc.perform(get("/prueba/parametro-validado").param("numero", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }

    @Test
    void rutaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void metodoNoPermitidoDevuelve405() throws Exception {
        mockMvc.perform(delete("/prueba/no-encontrado"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
    }
}
