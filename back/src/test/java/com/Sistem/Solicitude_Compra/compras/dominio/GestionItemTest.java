package com.Sistem.Solicitude_Compra.compras.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class GestionItemTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.now();

    @Test
    void naceEnPendienteConLaCadenaEnCursoYLaPrioridadDeLaUrgencia() {
        GestionItem g = gestion(Urgencia.ALTA);

        assertThat(g.getEstado()).isEqualTo(EstadoGestion.PENDIENTE);
        assertThat(g.getEstadoAprobacion()).isEqualTo(EstadoAprobacion.EN_CURSO);
        assertThat(g.getPrioridad()).isEqualTo((short) 1);
        assertThat(gestion(Urgencia.BAJA).getPrioridad()).isEqualTo((short) 3);
    }

    @Test
    void tomarPasaAEnCotizacionYSoloDesdePendiente() {
        GestionItem g = gestion(Urgencia.MEDIA);
        g.tomar(5L);

        assertThat(g.getEstado()).isEqualTo(EstadoGestion.EN_COTIZACION);
        assertThat(g.getEncargadoId()).isEqualTo(5L);
        assertThatThrownBy(() -> g.tomar(6L)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void reasignarCambiaDeEncargadoPeroNoEnUnItemCerrado() {
        GestionItem g = gestion(Urgencia.MEDIA);
        g.tomar(5L);
        g.reasignar(6L);
        assertThat(g.getEncargadoId()).isEqualTo(6L);

        g.rechazar("No hace falta", AHORA);
        assertThatThrownBy(() -> g.reasignar(7L)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void noSePuedeFijarPrecioSiNoEstaEnCotizacion() {
        GestionItem g = gestion(Urgencia.MEDIA);

        assertThatThrownBy(() -> g.fijarPrecioTotal(new BigDecimal("100"), Moneda.ARS, null))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void registrarCompraExigeLaCadenaAprobada() {
        GestionItem g = enCotizacionConPrecio();

        assertThatThrownBy(() -> g.registrarCompra(9L, AHORA))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("GESTION_SIN_APROBACION"));
    }

    @Test
    void registrarCompraExigePrecioTotalYProveedor() throws Exception {
        GestionItem sinPrecio = gestion(Urgencia.MEDIA);
        sinPrecio.tomar(5L);
        aprobar(sinPrecio);
        assertThatThrownBy(() -> sinPrecio.registrarCompra(9L, AHORA))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("GESTION_SIN_PRECIO"));

        GestionItem g = enCotizacionConPrecio();
        aprobar(g);
        assertThatThrownBy(() -> g.registrarCompra(null, AHORA))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("GESTION_SIN_PROVEEDOR"));
    }

    @Test
    void registrarCompraDejaElItemCompradoYResuelto() throws Exception {
        GestionItem g = enCotizacionConPrecio();
        aprobar(g);

        g.registrarCompra(9L, AHORA);

        assertThat(g.getEstado()).isEqualTo(EstadoGestion.COMPRADO);
        assertThat(g.getProveedorCompraId()).isEqualTo(9L);
        assertThat(g.getCompradoEn()).isEqualTo(AHORA);
        assertThat(g.getResueltoEn()).isEqualTo(AHORA);
    }

    @Test
    void corregirCompraSoloSiEstaComprado() throws Exception {
        GestionItem g = enCotizacionConPrecio();
        aprobar(g);
        assertThatThrownBy(() -> g.corregirCompra(new BigDecimal("90"), Moneda.ARS, 9L))
                .isInstanceOf(ReglaNegocioException.class);

        g.registrarCompra(9L, AHORA);
        g.corregirCompra(new BigDecimal("90"), Moneda.USD, 10L);

        assertThat(g.getPrecioTotal()).isEqualByComparingTo("90");
        assertThat(g.getMonedaTotal()).isEqualTo(Moneda.USD);
        assertThat(g.getProveedorCompraId()).isEqualTo(10L);
    }

    @Test
    void rechazarExigeMotivoYNoVaSobreUnItemComprado() throws Exception {
        GestionItem g = gestion(Urgencia.MEDIA);
        assertThatThrownBy(() -> g.rechazar(" ", AHORA)).isInstanceOf(ReglaNegocioException.class);

        g.rechazar("No se esperaba", AHORA);
        assertThat(g.getEstado()).isEqualTo(EstadoGestion.RECHAZADO);
        assertThat(g.getMotivoRechazo()).isEqualTo("No se esperaba");

        GestionItem comprado = enCotizacionConPrecio();
        aprobar(comprado);
        comprado.registrarCompra(9L, AHORA);
        assertThatThrownBy(() -> comprado.rechazar("tarde", AHORA)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void cancelarNoVaSobreUnItemRechazado() {
        GestionItem g = gestion(Urgencia.MEDIA);
        g.cancelar(AHORA);
        assertThat(g.getEstado()).isEqualTo(EstadoGestion.CANCELADO);

        GestionItem rechazado = gestion(Urgencia.MEDIA);
        rechazado.rechazar("no", AHORA);
        assertThatThrownBy(() -> rechazado.cancelar(AHORA)).isInstanceOf(ReglaNegocioException.class);
    }

    private static GestionItem gestion(Urgencia urgencia) {
        return new GestionItem(1L, 10L, 3L, 4L, TipoItem.PRODUCTO, "Resmas", new BigDecimal("2"), urgencia,
                LocalDate.now().plusDays(5), AHORA, new BigDecimal("1000"), Moneda.ARS);
    }

    private static GestionItem enCotizacionConPrecio() {
        GestionItem g = gestion(Urgencia.MEDIA);
        g.tomar(5L);
        g.fijarPrecioTotal(new BigDecimal("1200"), Moneda.ARS, null);
        return g;
    }

    // La cadena la aprueba el módulo Aprobaciones; acá se simula el valor que le copia el listener de Compras.
    private static void aprobar(GestionItem g) throws Exception {
        Field campo = GestionItem.class.getDeclaredField("estadoAprobacion");
        campo.setAccessible(true);
        campo.set(g, EstadoAprobacion.APROBADA);
    }
}
