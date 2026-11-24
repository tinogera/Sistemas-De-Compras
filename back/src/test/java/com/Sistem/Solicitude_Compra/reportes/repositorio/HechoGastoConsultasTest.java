package com.Sistem.Solicitude_Compra.reportes.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.reportes.dominio.HechoGasto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class HechoGastoConsultasTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 30);

    @Autowired HechoGastoRepository gastos;

    private long siguienteId = 1;

    @BeforeEach
    void limpiar() {
        gastos.deleteAll();
    }

    private void gasto(LocalDate fecha, long sector, long categoria, long proveedor, Moneda moneda, String importe,
                       String envio) {
        HechoGasto g = new HechoGasto(siguienteId, siguienteId, fecha, sector, categoria, proveedor, moneda,
                new BigDecimal(importe));
        siguienteId++;
        g.actualizarMontos(new BigDecimal(importe), envio == null ? null : new BigDecimal(envio));
        gastos.saveAndFlush(g);
    }

    private static FilaGasto fila(List<FilaGasto> filas, long clave, Moneda moneda) {
        return filas.stream().filter(f -> f.getClave() == clave && f.getMoneda() == moneda).findFirst().orElseThrow();
    }

    @Test
    void sumaTotalYEnvioPorSectorYCuentaLasOrdenes() {
        gasto(LocalDate.of(2026, 11, 5), 1, 10, 100, Moneda.ARS, "1000.00", "50.00");
        gasto(LocalDate.of(2026, 11, 6), 1, 10, 100, Moneda.ARS, "500.00", null);
        gasto(LocalDate.of(2026, 11, 7), 2, 10, 100, Moneda.ARS, "300.00", "10.00");

        List<FilaGasto> filas = gastos.gastoPorSector(DESDE, HASTA);

        assertThat(filas).hasSize(2);
        FilaGasto sector1 = fila(filas, 1, Moneda.ARS);
        assertThat(sector1.getTotal()).isEqualByComparingTo("1500.00");
        assertThat(sector1.getEnvio()).isEqualByComparingTo("50.00");
        assertThat(sector1.getCantidad()).isEqualTo(2);
        assertThat(fila(filas, 2, Moneda.ARS).getTotal()).isEqualByComparingTo("300.00");
    }

    @Test
    void pesosYDolaresNuncaSeSuman() {
        gasto(LocalDate.of(2026, 11, 5), 1, 10, 100, Moneda.ARS, "1000.00", "50.00");
        gasto(LocalDate.of(2026, 11, 6), 1, 10, 100, Moneda.USD, "200.00", "5.00");

        List<FilaGasto> filas = gastos.gastoPorSector(DESDE, HASTA);

        assertThat(filas).hasSize(2);
        assertThat(fila(filas, 1, Moneda.ARS).getTotal()).isEqualByComparingTo("1000.00");
        assertThat(fila(filas, 1, Moneda.ARS).getEnvio()).isEqualByComparingTo("50.00");
        assertThat(fila(filas, 1, Moneda.USD).getTotal()).isEqualByComparingTo("200.00");
        assertThat(fila(filas, 1, Moneda.USD).getEnvio()).isEqualByComparingTo("5.00");
    }

    @Test
    void elRangoIncluyeLasDosPuntasYExcluyeLoDeAfuera() {
        gasto(DESDE.minusDays(1), 1, 10, 100, Moneda.ARS, "1.00", null);
        gasto(DESDE, 1, 10, 100, Moneda.ARS, "10.00", null);
        gasto(HASTA, 1, 10, 100, Moneda.ARS, "100.00", null);
        gasto(HASTA.plusDays(1), 1, 10, 100, Moneda.ARS, "1000.00", null);

        List<FilaGasto> filas = gastos.gastoPorSector(DESDE, HASTA);

        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getTotal()).isEqualByComparingTo("110.00");
        assertThat(filas.get(0).getCantidad()).isEqualTo(2);
    }

    @Test
    void agrupaPorCategoriaYPorProveedor() {
        gasto(LocalDate.of(2026, 11, 5), 1, 10, 100, Moneda.ARS, "1000.00", null);
        gasto(LocalDate.of(2026, 11, 6), 2, 10, 200, Moneda.ARS, "500.00", null);
        gasto(LocalDate.of(2026, 11, 7), 3, 20, 200, Moneda.ARS, "300.00", null);

        List<FilaGasto> porCategoria = gastos.gastoPorCategoria(DESDE, HASTA);
        List<FilaGasto> porProveedor = gastos.gastoPorProveedor(DESDE, HASTA);

        assertThat(fila(porCategoria, 10, Moneda.ARS).getTotal()).isEqualByComparingTo("1500.00");
        assertThat(fila(porCategoria, 20, Moneda.ARS).getTotal()).isEqualByComparingTo("300.00");
        assertThat(fila(porProveedor, 100, Moneda.ARS).getTotal()).isEqualByComparingTo("1000.00");
        assertThat(fila(porProveedor, 200, Moneda.ARS).getTotal()).isEqualByComparingTo("800.00");
    }

    @Test
    void sinGastosDevuelveUnaListaVacia() {
        assertThat(gastos.gastoPorSector(DESDE, HASTA)).isEmpty();
        assertThat(gastos.gastoPorCategoria(DESDE, HASTA)).isEmpty();
        assertThat(gastos.gastoPorProveedor(DESDE, HASTA)).isEmpty();
    }
}
