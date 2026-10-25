package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.OperadorCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Regla;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.ReglaCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.ReglaPaso;
import com.Sistem.Solicitude_Compra.aprobaciones.repository.ReglaRepository;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** Tests unitarios del motor: sin Spring ni base de datos. El evaluador de monto es el real. */
@ExtendWith(MockitoExtension.class)
class MotorDeReglasTest {

    private static final Long SECTOR_GERENCIA = 10L;

    @Mock
    private ReglaRepository reglaRepository;

    private MotorDeReglas motor;

    @BeforeEach
    void iniciar() {
        motor = new MotorDeReglas(reglaRepository, List.of(new EvaluadorMonto()));
    }

    // ---------- casos de la tabla del diseño ----------

    @Test
    void caso1_600000ArsConReglaGrandesGanaLaCadenaDeDosPasos() {
        Regla grandes = grandes(1, true);
        repositorioDevuelve(grandes, porDefecto());

        ResultadoEvaluacion resultado = motor.evaluar(contexto("600000", Moneda.ARS));

        assertSame(grandes, resultado.regla());
        assertEquals(2, resultado.pasos().size());

        PasoRequerido primero = resultado.pasos().get(0);
        assertEquals(1, primero.orden());
        assertEquals(TipoAprobador.ROL, primero.tipoAprobador());
        assertEquals(Rol.SUPERVISOR, primero.rol());
        assertEquals("Supervisor", primero.nombre());

        PasoRequerido segundo = resultado.pasos().get(1);
        assertEquals(2, segundo.orden());
        assertEquals(TipoAprobador.SECTOR, segundo.tipoAprobador());
        assertEquals(SECTOR_GERENCIA, segundo.sectorId());
    }

    @Test
    void caso2_500000ArsNoCumpleMayorEstrictoYCaeEnLaPorDefecto() {
        Regla porDefecto = porDefecto();
        repositorioDevuelve(grandes(1, true), porDefecto);

        ResultadoEvaluacion resultado = motor.evaluar(contexto("500000", Moneda.ARS));

        assertSame(porDefecto, resultado.regla());
        assertEquals(1, resultado.pasos().size());
        assertEquals(Rol.SUPERVISOR, resultado.pasos().get(0).rol());
    }

    @Test
    void caso3_30000ArsConReglaChicasEsAprobacionAutomaticaSinPasos() {
        Regla chicas = reglaMonto("Chicas", 1, OperadorCondicion.MENOR_IGUAL, "50000", Moneda.ARS);
        repositorioDevuelve(chicas, porDefecto());

        ResultadoEvaluacion resultado = motor.evaluar(contexto("30000", Moneda.ARS));

        assertSame(chicas, resultado.regla());
        assertTrue(resultado.pasos().isEmpty());
    }

    @Test
    void caso4_1000UsdConReglasEnArsCaeEnLaPorDefecto() {
        Regla porDefecto = porDefecto();
        repositorioDevuelve(grandes(1, true), porDefecto);

        ResultadoEvaluacion resultado = motor.evaluar(contexto("1000", Moneda.USD));

        assertSame(porDefecto, resultado.regla());
        assertEquals(1, resultado.pasos().size());
    }

    @Test
    void caso5_dosReglasQueCumplenGanaLaDePrioridad5() {
        Regla prioridad10 = reglaMonto("Prioridad 10", 10, OperadorCondicion.MAYOR, "100000", Moneda.ARS,
                pasoRol(1, Rol.ENCARGADO));
        Regla prioridad5 = reglaMonto("Prioridad 5", 5, OperadorCondicion.MAYOR, "500000", Moneda.ARS,
                pasoRol(1, Rol.SUPERVISOR));
        // Se pasan desordenadas: el repositorio simulado las devuelve por prioridad ascendente.
        repositorioDevuelve(prioridad10, porDefecto(), prioridad5);

        ResultadoEvaluacion resultado = motor.evaluar(contexto("600000", Moneda.ARS));

        assertSame(prioridad5, resultado.regla());
    }

    @Test
    void caso6_reglaQueCumpleEstandoInactivaSeIgnoraYCaeEnLaPorDefecto() {
        Regla grandesInactiva = grandes(1, false);
        Regla porDefecto = porDefecto();
        // El repositorio simulado filtra las inactivas, como findByActivaTrue...
        repositorioDevuelve(grandesInactiva, porDefecto);

        ResultadoEvaluacion resultado = motor.evaluar(contexto("600000", Moneda.ARS));

        assertSame(porDefecto, resultado.regla());
        assertEquals(1, resultado.pasos().size());
    }

    // ---------- sin regla aplicable ----------

    @Test
    void sinReglasActivasLanzaIllegalStateException() {
        repositorioDevuelve();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> motor.evaluar(contexto("600000", Moneda.ARS)));

        assertTrue(error.getMessage().contains("por defecto"));
    }

    @Test
    void siNingunaReglaCumpleLanzaIllegalStateException() {
        // No hay regla por defecto (sin condiciones) y la única regla no cumple.
        repositorioDevuelve(grandes(1, true));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> motor.evaluar(contexto("1000", Moneda.ARS)));

        assertTrue(error.getMessage().contains("por defecto"));
    }

    // ---------- ayudas ----------

    /** Simula findByActivaTrueOrderByPrioridadAsc: solo las activas, por prioridad ascendente. */
    private void repositorioDevuelve(Regla... reglas) {
        List<Regla> activas = Arrays.stream(reglas)
                .filter(Regla::isActiva)
                .sorted(Comparator.comparingInt(Regla::getPrioridad))
                .toList();
        when(reglaRepository.findByActivaTrueOrderByPrioridadAsc()).thenReturn(activas);
    }

    private static ContextoEvaluacion contexto(String importe, Moneda moneda) {
        return new ContextoEvaluacion(new Monto(new BigDecimal(importe), moneda), 1L, 1L, 1L, Urgencia.MEDIA);
    }

    /** "Grandes": MONTO MAYOR 500.000 ARS → Supervisor (1), Gerencia General (2). */
    private static Regla grandes(int prioridad, boolean activa) {
        Regla regla = reglaMonto("Grandes", prioridad, OperadorCondicion.MAYOR, "500000", Moneda.ARS,
                pasoRol(1, Rol.SUPERVISOR), pasoSector(2, SECTOR_GERENCIA));
        regla.setActiva(activa);
        return regla;
    }

    /** Regla por defecto: sin condiciones (siempre se cumple), un paso SUPERVISOR. */
    private static Regla porDefecto() {
        Regla regla = new Regla();
        regla.setNombre("Por defecto");
        regla.setPrioridad(1000);
        regla.setPorDefecto(true);
        regla.agregarPaso(pasoRol(1, Rol.SUPERVISOR));
        return regla;
    }

    private static Regla reglaMonto(String nombre, int prioridad, OperadorCondicion operador,
                                    String valor, Moneda moneda, ReglaPaso... pasos) {
        Regla regla = new Regla();
        regla.setNombre(nombre);
        regla.setPrioridad(prioridad);

        ReglaCondicion condicion = new ReglaCondicion();
        condicion.setTipo(TipoCondicion.MONTO);
        condicion.setOperador(operador);
        condicion.setValorNumerico(new BigDecimal(valor));
        condicion.setMoneda(moneda);
        regla.agregarCondicion(condicion);

        for (ReglaPaso paso : pasos) {
            regla.agregarPaso(paso);
        }
        return regla;
    }

    private static ReglaPaso pasoRol(int orden, Rol rol) {
        ReglaPaso paso = new ReglaPaso();
        paso.setOrden(orden);
        paso.setTipoAprobador(TipoAprobador.ROL);
        paso.setRol(rol);
        return paso;
    }

    private static ReglaPaso pasoSector(int orden, Long sectorId) {
        ReglaPaso paso = new ReglaPaso();
        paso.setOrden(orden);
        paso.setTipoAprobador(TipoAprobador.SECTOR);
        paso.setSectorId(sectorId);
        return paso;
    }
}