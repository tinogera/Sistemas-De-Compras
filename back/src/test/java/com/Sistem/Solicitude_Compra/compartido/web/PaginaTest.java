package com.Sistem.Solicitude_Compra.compartido.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = PaginaTest.ControladorPrueba.class)
@Import(PaginaTest.ControladorPrueba.class)
class PaginaTest {

    @Autowired
    MockMvc mockMvc;

    @RestController
    static class ControladorPrueba {

        // Devuelve los datos que recibió en el Pageable, para ver cómo lo armó Spring.
        @GetMapping("/prueba/paginado")
        Pagina<String> listar(Pageable pageable) {
            return Pagina.de(new PageImpl<>(List.of("a", "b"), pageable, 45));
        }
    }

    @Test
    void copiaLosDatosDelPage() {
        Page<String> page = new PageImpl<>(List.of("c", "d"), PageRequest.of(1, 2), 5);

        Pagina<String> pagina = Pagina.de(page);

        assertThat(pagina.contenido()).containsExactly("c", "d");
        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.tamanio()).isEqualTo(2);
        assertThat(pagina.totalElementos()).isEqualTo(5);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
    }

    @Test
    void pageVacio() {
        Pagina<String> pagina = Pagina.de(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(pagina.contenido()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
        assertThat(pagina.totalPaginas()).isZero();
    }

    @Test
    void jsonConLosNombresQueEsperaElFront() throws Exception {
        mockMvc.perform(get("/prueba/paginado").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(2))
                .andExpect(jsonPath("$.pagina").value(2))
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.totalElementos").value(45))
                .andExpect(jsonPath("$.totalPaginas").value(5));
    }

    @Test
    void sinParametrosUsaPagina0De20() throws Exception {
        mockMvc.perform(get("/prueba/paginado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanio").value(20));
    }

    @Test
    void tamanioMayorA100SeLimitaA100() throws Exception {
        mockMvc.perform(get("/prueba/paginado").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanio").value(100));
    }
}
