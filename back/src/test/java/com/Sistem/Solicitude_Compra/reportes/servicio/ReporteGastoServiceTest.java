package com.Sistem.Solicitude_Compra.reportes.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.catalogo.ProveedorResumen;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.reportes.dominio.Agrupacion;
import com.Sistem.Solicitude_Compra.reportes.repositorio.FilaGasto;
import com.Sistem.Solicitude_Compra.reportes.repositorio.HechoGastoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReporteGastoServiceTest {

    static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    static final LocalDate HASTA = LocalDate.of(2026, 11, 30);

    HechoGastoRepository gastos = mock(HechoGastoRepository.class);
    AuthApi authApi = mock(AuthApi.class);
    CatalogoApi catalogoApi = mock(CatalogoApi.class);
    ReporteGastoService service = new ReporteGastoService(gastos, authApi, catalogoApi);

    /** Fila de prueba hecha a mano: crear el mock adentro de un when(...) anidado rompe el stubbing de Mockito. */
    private record FilaDePrueba(Long clave, Moneda moneda, BigDecimal total, BigDecimal envio, Long cantidad)
            implements FilaGasto {
        @Override public Long getClave() { return clave; }
        @Override public Moneda getMoneda() { return moneda; }
        @Override public BigDecimal getTotal() { return total; }
        @Override public BigDecimal getEnvio() { return envio; }
        @Override public Long getCantidad() { return cantidad; }
    }

    private static FilaGasto fila(Long clave, Moneda moneda, String total, String envio, long cantidad) {
        return new FilaDePrueba(clave, moneda, new BigDecimal(total), new BigDecimal(envio), cantidad);
    }

    @Test
    void porSectorPideLosNombresAAuthYDevuelveTotalYEnvioSeparados() {
        when(gastos.gastoPorSector(DESDE, HASTA)).thenReturn(List.of(fila(1L, Moneda.ARS, "1500.00", "50.00", 2)));
        when(authApi.nombreSector(1L)).thenReturn("Sistemas");

        List<GastoAgrupado> resultado = service.gasto(Agrupacion.SECTOR, DESDE, HASTA);

        assertThat(resultado).hasSize(1);
        GastoAgrupado g = resultado.get(0);
        assertThat(g.clave()).isEqualTo(1L);
        assertThat(g.nombre()).isEqualTo("Sistemas");
        assertThat(g.moneda()).isEqualTo(Moneda.ARS);
        assertThat(g.total()).isEqualByComparingTo("1500.00");
        assertThat(g.envio()).isEqualByComparingTo("50.00");
        assertThat(g.cantidadOrdenes()).isEqualTo(2);
    }

    @Test
    void porCategoriaUsaCatalogo() {
        when(gastos.gastoPorCategoria(DESDE, HASTA)).thenReturn(List.of(fila(10L, Moneda.USD, "200.00", "0", 1)));
        when(catalogoApi.nombreCategoria(10L)).thenReturn("Papelería");

        assertThat(service.gasto(Agrupacion.CATEGORIA, DESDE, HASTA)).extracting(GastoAgrupado::nombre)
                .containsExactly("Papelería");
    }

    @Test
    void porProveedorUsaLaRazonSocial() {
        when(gastos.gastoPorProveedor(DESDE, HASTA)).thenReturn(List.of(fila(100L, Moneda.ARS, "10.00", "0", 1)));
        when(catalogoApi.obtenerProveedor(100L)).thenReturn(new ProveedorResumen(100L, "Papelera SA", true));

        assertThat(service.gasto(Agrupacion.PROVEEDOR, DESDE, HASTA)).extracting(GastoAgrupado::nombre)
                .containsExactly("Papelera SA");
    }

    @Test
    void elMismoSectorEnPesosYDolaresSalePorSeparadoYPideElNombreUnaVez() {
        when(gastos.gastoPorSector(DESDE, HASTA)).thenReturn(List.of(
                fila(1L, Moneda.USD, "200.00", "5.00", 1), fila(1L, Moneda.ARS, "1000.00", "50.00", 3)));
        when(authApi.nombreSector(1L)).thenReturn("Sistemas");

        List<GastoAgrupado> resultado = service.gasto(Agrupacion.SECTOR, DESDE, HASTA);

        assertThat(resultado).extracting(GastoAgrupado::moneda).containsExactlyInAnyOrder(Moneda.ARS, Moneda.USD);
        verify(authApi, times(1)).nombreSector(1L);
    }

    @Test
    void ordenaPorNombre() {
        when(gastos.gastoPorSector(DESDE, HASTA)).thenReturn(List.of(
                fila(1L, Moneda.ARS, "1.00", "0", 1), fila(2L, Moneda.ARS, "1.00", "0", 1)));
        when(authApi.nombreSector(1L)).thenReturn("Zeta");
        when(authApi.nombreSector(2L)).thenReturn("Alfa");

        assertThat(service.gasto(Agrupacion.SECTOR, DESDE, HASTA)).extracting(GastoAgrupado::nombre)
                .containsExactly("Alfa", "Zeta");
    }

    @Test
    void siFaltaElNombreElGastoIgualAparece() {
        when(gastos.gastoPorSector(DESDE, HASTA)).thenReturn(List.of(fila(9L, Moneda.ARS, "1.00", "0", 1)));
        when(authApi.nombreSector(9L)).thenThrow(new NoEncontradoException("No existe el sector 9"));

        assertThat(service.gasto(Agrupacion.SECTOR, DESDE, HASTA)).extracting(GastoAgrupado::nombre)
                .containsExactly("Desconocido (9)");
    }

    @Test
    void desdePosteriorAHastaEsUnParametroInvalido() {
        assertThatThrownBy(() -> service.gasto(Agrupacion.SECTOR, HASTA, DESDE))
                .isInstanceOf(ParametroInvalidoException.class);
        verify(gastos, never()).gastoPorSector(HASTA, DESDE);
    }

    @Test
    void desdeIgualAHastaEsValido() {
        when(gastos.gastoPorSector(DESDE, DESDE)).thenReturn(List.of());

        assertThat(service.gasto(Agrupacion.SECTOR, DESDE, DESDE)).isEmpty();
    }

    @Test
    void faltarFechasOAgruparEsUnParametroInvalido() {
        assertThatThrownBy(() -> service.gasto(Agrupacion.SECTOR, null, HASTA))
                .isInstanceOf(ParametroInvalidoException.class);
        assertThatThrownBy(() -> service.gasto(Agrupacion.SECTOR, DESDE, null))
                .isInstanceOf(ParametroInvalidoException.class);
        assertThatThrownBy(() -> service.gasto(null, DESDE, HASTA)).isInstanceOf(ParametroInvalidoException.class);
    }

    @Test
    void agruparAceptaMinusculasYRechazaLoDemas() {
        assertThat(Agrupacion.desde("sector")).isEqualTo(Agrupacion.SECTOR);
        assertThat(Agrupacion.desde("Categoria")).isEqualTo(Agrupacion.CATEGORIA);
        assertThat(Agrupacion.desde(" PROVEEDOR ")).isEqualTo(Agrupacion.PROVEEDOR);
        assertThatThrownBy(() -> Agrupacion.desde("usuario")).isInstanceOf(ParametroInvalidoException.class);
        assertThatThrownBy(() -> Agrupacion.desde(null)).isInstanceOf(ParametroInvalidoException.class);
    }
}
