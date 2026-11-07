package com.Sistem.Solicitude_Compra.catalogo.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import org.junit.jupiter.api.Test;

class CategoriaTransicionesTest {

    private static Categoria categoria(Long id, TipoItem tipo, EstadoCategoria estado) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre("Cat " + id);
        c.setTipo(tipo);
        c.setEstado(estado);
        return c;
    }

    @Test
    void nuevaPasaAConocida() {
        Categoria c = categoria(1L, TipoItem.PRODUCTO, EstadoCategoria.NUEVA);
        c.confirmar();
        assertThat(c.getEstado()).isEqualTo(EstadoCategoria.CONOCIDA);
    }

    @Test
    void conocidaYUnificadaNoSePuedenConfirmar() {
        for (EstadoCategoria estado : new EstadoCategoria[] {EstadoCategoria.CONOCIDA, EstadoCategoria.UNIFICADA}) {
            Categoria c = categoria(1L, TipoItem.PRODUCTO, estado);
            assertThatThrownBy(c::confirmar)
                    .isInstanceOfSatisfying(ReglaNegocioException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo("TRANSICION_INVALIDA"));
            assertThat(c.getEstado()).isEqualTo(estado);
        }
    }

    @Test
    void unificarDejaElOrigenUnificadoEnElDestino() {
        Categoria origen = categoria(1L, TipoItem.PRODUCTO, EstadoCategoria.NUEVA);
        Categoria destino = categoria(2L, TipoItem.PRODUCTO, EstadoCategoria.CONOCIDA);
        origen.unificarEn(destino);
        assertThat(origen.getEstado()).isEqualTo(EstadoCategoria.UNIFICADA);
        assertThat(origen.getUnificadaEn()).isSameAs(destino);
        assertThat(destino.getEstado()).isEqualTo(EstadoCategoria.CONOCIDA);
    }

    @Test
    void noSePuedeUnificarConsigoMismaNiConOtroTipoNiContraUnaUnificada() {
        Categoria origen = categoria(1L, TipoItem.PRODUCTO, EstadoCategoria.NUEVA);
        assertThatThrownBy(() -> origen.unificarEn(origen)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> origen.unificarEn(categoria(2L, TipoItem.SERVICIO, EstadoCategoria.CONOCIDA)))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> origen.unificarEn(categoria(3L, TipoItem.PRODUCTO, EstadoCategoria.UNIFICADA)))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(origen.getEstado()).isEqualTo(EstadoCategoria.NUEVA);
        assertThat(origen.getUnificadaEn()).isNull();
    }

    @Test
    void unaCategoriaYaUnificadaNoSeUnificaOtraVez() {
        Categoria origen = categoria(1L, TipoItem.PRODUCTO, EstadoCategoria.UNIFICADA);
        assertThatThrownBy(() -> origen.unificarEn(categoria(2L, TipoItem.PRODUCTO, EstadoCategoria.CONOCIDA)))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("TRANSICION_INVALIDA"));
    }
}
