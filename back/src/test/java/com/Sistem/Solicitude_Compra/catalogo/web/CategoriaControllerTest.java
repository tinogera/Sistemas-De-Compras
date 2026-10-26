package com.Sistem.Solicitude_Compra.catalogo.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.EstadoCategoria;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.CategoriaRepository;
import com.Sistem.Solicitude_Compra.catalogo.servicio.CategoriaService;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.web.ManejadorErrores;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CategoriaControllerTest {
    private CategoriaRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void iniciar() {
        repository = mock(CategoriaRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new CategoriaController(new CategoriaService(repository)))
                .setControllerAdvice(new ManejadorErrores())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }

    @Test
    void autocompletadoRecortaTextoYDevuelveDTO() throws Exception {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Globos");
        categoria.setTipo(TipoItem.PRODUCTO);
        when(repository.findTop10ByTipoAndNombreContainingIgnoreCaseAndEstadoNot(
                TipoItem.PRODUCTO, "GLO", EstadoCategoria.UNIFICADA, Sort.by("nombre")))
                .thenReturn(List.of(categoria));

        mvc.perform(get("/api/v1/categorias").param("tipo", "PRODUCTO").param("q", " GLO "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Globos"))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tipo").value("PRODUCTO"))
                .andExpect(jsonPath("$[0].estado").value("NUEVA"))
                .andExpect(jsonPath("$[0].unificadaEn").doesNotExist());
    }

    @Test
    void textoCortoOVacioNoConsultaLaBase() throws Exception {
        for (String q : List.of("g", "  g  ", "", "   ")) {
            mvc.perform(get("/api/v1/categorias").param("tipo", "PRODUCTO").param("q", q))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void filtrosInvalidosDevuelven400() throws Exception {
        mvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
        mvc.perform(get("/api/v1/categorias").param("q", "gl"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
        mvc.perform(get("/api/v1/categorias").param("estado", "INVALIDO"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
        verifyNoInteractions(repository);
    }

    @Test
    void estadoDevuelvePagina() throws Exception {
        var pagina = PageRequest.of(1, 2, Sort.by("nombre"));
        when(repository.findByEstado(EstadoCategoria.NUEVA, pagina))
                .thenReturn(new PageImpl<>(List.of(), pagina, 2));
        mvc.perform(get("/api/v1/categorias").param("estado", "NUEVA")
                        .param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanio").value(2))
                .andExpect(jsonPath("$.totalElementos").value(2));
    }
}