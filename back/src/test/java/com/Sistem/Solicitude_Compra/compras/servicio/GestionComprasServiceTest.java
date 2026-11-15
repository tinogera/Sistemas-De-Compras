package com.Sistem.Solicitude_Compra.compras.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.catalogo.ProveedorResumen;
import com.Sistem.Solicitude_Compra.compartido.errores.ErrorNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraRegistrada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CotizacionCargada;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemAsignado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.eventos.PrecioTotalActualizado;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoCotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.CotizacionRepository;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import com.Sistem.Solicitude_Compra.compras.web.CargarCotizacionRequest;
import com.Sistem.Solicitude_Compra.compras.web.ComprarRequest;
import com.Sistem.Solicitude_Compra.compras.web.EditarCompraRequest;
import com.Sistem.Solicitude_Compra.compras.web.PrecioTotalRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

class GestionComprasServiceTest {

    private static final Long ITEM = 1L;
    private static final Long YO = 5L;
    private static final Long OTRO = 6L;
    private static final Long VERSION = 3L;
    private static final LocalDate HOY = LocalDate.now(Cotizacion.ZONA);

    private GestionItemRepository gestiones;
    private CotizacionRepository cotizaciones;
    private CatalogoApi catalogo;
    private ApplicationEventPublisher eventos;
    private GestionComprasService service;

    @BeforeEach
    void iniciar() {
        gestiones = mock(GestionItemRepository.class);
        cotizaciones = mock(CotizacionRepository.class);
        catalogo = mock(CatalogoApi.class);
        eventos = mock(ApplicationEventPublisher.class);
        service = new GestionComprasService(gestiones, cotizaciones, catalogo, eventos);
        when(gestiones.save(any(GestionItem.class))).thenAnswer(i -> i.getArgument(0));
        when(cotizaciones.save(any(Cotizacion.class))).thenAnswer(i -> i.getArgument(0));
        when(cotizaciones.saveAndFlush(any(Cotizacion.class))).thenAnswer(i -> i.getArgument(0));
        when(catalogo.obtenerProveedor(9L)).thenReturn(new ProveedorResumen(9L, "Globos SA", true));
    }

    // ---- helpers ----

    private GestionItem pendiente() {
        GestionItem g = new GestionItem(ITEM, 10L, 3L, 4L, TipoItem.PRODUCTO, "Resmas", new BigDecimal("2.00"),
                Urgencia.ALTA, HOY.plusDays(5), OffsetDateTime.now(), new BigDecimal("1000.00"), Moneda.ARS);
        ReflectionTestUtils.setField(g, "version", VERSION);
        when(gestiones.findById(ITEM)).thenReturn(Optional.of(g));
        return g;
    }

    private GestionItem enCotizacion() {
        GestionItem g = pendiente();
        g.tomar(YO);
        return g;
    }

    private Cotizacion cotizacion(Long id, Long itemId, LocalDate validez) {
        Cotizacion c = new Cotizacion(itemId, 9L, "Globos SA", Moneda.ARS, new BigDecimal("450.00"),
                new BigDecimal("900.00"), validez, null, null, YO, OffsetDateTime.now());
        ReflectionTestUtils.setField(c, "id", id);
        when(cotizaciones.findByIdAndItemId(id, itemId)).thenReturn(Optional.of(c));
        when(cotizaciones.findById(id)).thenReturn(Optional.of(c));
        return c;
    }

    private <T> T evento(Class<T> tipo) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventos, org.mockito.Mockito.atLeastOnce()).publishEvent(captor.capture());
        return captor.getAllValues().stream().filter(tipo::isInstance).map(tipo::cast).findFirst().orElseThrow();
    }

    private static String codigo(Runnable accion) {
        try {
            accion.run();
        } catch (ErrorNegocioException e) {
            return e.getCodigo();
        }
        return null;
    }

    // ---- T09.4 · tomar y reasignar ----

    @Test
    void tomarPublicaItemAsignadoSinEncargadoAnterior() {
        GestionItem g = pendiente();
        service.tomar(ITEM, YO);

        assertThat(g.getEstado()).isEqualTo(EstadoGestion.EN_COTIZACION);
        ItemAsignado e = evento(ItemAsignado.class);
        assertThat(e.encargadoAnteriorId()).isNull();
        assertThat(e.encargadoNuevoId()).isEqualTo(YO);
        assertThat(e.itemId()).isEqualTo(ITEM);
    }

    @Test
    void tomarUnItemPropioNoPublicaNada() {
        enCotizacion();
        service.tomar(ITEM, YO);
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void tomarUnItemDeOtroDa409YaAsignado() {
        enCotizacion();
        assertThat(codigo(() -> service.tomar(ITEM, OTRO))).isEqualTo("YA_ASIGNADO");
    }

    @Test
    void reasignarPasaElItemAlQueLoPideYPublicaAnteriorYNuevo() {
        GestionItem g = enCotizacion();
        service.reasignar(ITEM, OTRO);

        assertThat(g.getEncargadoId()).isEqualTo(OTRO);
        ItemAsignado e = evento(ItemAsignado.class);
        assertThat(e.encargadoAnteriorId()).isEqualTo(YO);
        assertThat(e.encargadoNuevoId()).isEqualTo(OTRO);
    }

    @Test
    void itemInexistenteDa404() {
        when(gestiones.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.tomar(99L, YO)).isInstanceOf(NoEncontradoException.class);
    }

    // ---- T10.2 · cargar cotización ----

    private CargarCotizacionRequest pedidoCotizacion(Long proveedor, BigDecimal unitario, LocalDate validez) {
        return new CargarCotizacionRequest(proveedor, Moneda.USD, unitario, validez, null, "obs");
    }

    @Test
    void cargarCotizacionCalculaElTotalConBigDecimalYPublicaElEvento() {
        enCotizacion();
        Cotizacion c = service.cargarCotizacion(ITEM, pedidoCotizacion(9L, new BigDecimal("10.555"), HOY), YO);

        // unitario redondeado a 2 decimales (10.56) × cantidad 2.00 = 21.12
        assertThat(c.getPrecioUnitario()).isEqualByComparingTo("10.56");
        assertThat(c.getPrecioTotal()).isEqualByComparingTo("21.12");
        assertThat(c.getEstado()).isEqualTo(EstadoCotizacion.CANDIDATA);
        assertThat(c.getProveedorRazonSocial()).isEqualTo("Globos SA");
        assertThat(c.getCargadaPor()).isEqualTo(YO);
        CotizacionCargada e = evento(CotizacionCargada.class);
        assertThat(e.precioTotal()).isEqualByComparingTo("21.12");
        assertThat(e.moneda()).isEqualTo(Moneda.USD);
        assertThat(e.proveedorId()).isEqualTo(9L);
    }

    @Test
    void cargarCotizacionExigeItemEnCotizacionYAsignadoAMi() {
        pendiente();
        assertThat(codigo(() -> service.cargarCotizacion(ITEM, pedidoCotizacion(9L, BigDecimal.TEN, HOY), YO)))
                .isEqualTo("GESTION_ESTADO_INVALIDO");

        enCotizacion();
        assertThatThrownBy(() -> service.cargarCotizacion(ITEM, pedidoCotizacion(9L, BigDecimal.TEN, HOY), OTRO))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void cargarCotizacionConProveedorInactivoOInexistenteDa422() {
        enCotizacion();
        when(catalogo.obtenerProveedor(8L)).thenReturn(new ProveedorResumen(8L, "Baja SA", false));
        when(catalogo.obtenerProveedor(7L)).thenThrow(new NoEncontradoException("No existe"));

        assertThat(codigo(() -> service.cargarCotizacion(ITEM, pedidoCotizacion(8L, BigDecimal.TEN, HOY), YO)))
                .isEqualTo("PROVEEDOR_INACTIVO");
        assertThat(codigo(() -> service.cargarCotizacion(ITEM, pedidoCotizacion(7L, BigDecimal.TEN, HOY), YO)))
                .isEqualTo("PROVEEDOR_INACTIVO");
        verify(cotizaciones, never()).save(any());
    }

    @Test
    void cargarCotizacionYaVencidaOConPrecioQueRedondeaACeroDa400() {
        enCotizacion();
        assertThatThrownBy(() -> service.cargarCotizacion(ITEM,
                pedidoCotizacion(9L, BigDecimal.TEN, HOY.minusDays(1)), YO))
                .isInstanceOf(ParametroInvalidoException.class);
        assertThatThrownBy(() -> service.cargarCotizacion(ITEM,
                pedidoCotizacion(9L, new BigDecimal("0.001"), HOY), YO))
                .isInstanceOf(ParametroInvalidoException.class);
    }

    // ---- T10.3 · precio total ----

    @Test
    void elegirUnaCotizacionFijaElPrecioYPublicaElEvento() {
        GestionItem g = enCotizacion();
        Cotizacion c = cotizacion(50L, ITEM, HOY.plusDays(3));

        service.elegirCotizacion(ITEM, 50L, VERSION, YO);

        assertThat(c.getEstado()).isEqualTo(EstadoCotizacion.SELECCIONADA);
        assertThat(g.getPrecioTotal()).isEqualByComparingTo("900.00");
        assertThat(g.getMonedaTotal()).isEqualTo(Moneda.ARS);
        assertThat(g.getCotizacionElegidaId()).isEqualTo(50L);
        PrecioTotalActualizado e = evento(PrecioTotalActualizado.class);
        assertThat(e.precioTotal()).isEqualByComparingTo("900.00");
        assertThat(e.moneda()).isEqualTo(Moneda.ARS);
        assertThat(e.cotizacionId()).isEqualTo(50L);
        assertThat(e.proveedorRazonSocial()).isEqualTo("Globos SA");
        assertThat(e.usuarioId()).isEqualTo(YO);
    }

    @Test
    void elegirOtraCotizacionDejaUnaSolaSeleccionada() {
        enCotizacion();
        Cotizacion vieja = cotizacion(50L, ITEM, HOY.plusDays(3));
        vieja.seleccionar();
        Cotizacion nueva = cotizacion(51L, ITEM, HOY.plusDays(3));
        when(cotizaciones.findByItemIdAndEstado(ITEM, EstadoCotizacion.SELECCIONADA)).thenReturn(Optional.of(vieja));

        service.elegirCotizacion(ITEM, 51L, VERSION, YO);

        assertThat(vieja.getEstado()).isEqualTo(EstadoCotizacion.CANDIDATA);
        assertThat(nueva.getEstado()).isEqualTo(EstadoCotizacion.SELECCIONADA);
        // La anterior se baja y se manda a la base ANTES de elegir la nueva (índice único ux_una_seleccionada).
        var orden = org.mockito.Mockito.inOrder(cotizaciones);
        orden.verify(cotizaciones).saveAndFlush(vieja);
        orden.verify(cotizaciones).save(nueva);
    }

    @Test
    void elegirUnaCotizacionVencidaDa422YNoCambiaNada() {
        GestionItem g = enCotizacion();
        cotizacion(50L, ITEM, HOY.minusDays(1));

        assertThat(codigo(() -> service.elegirCotizacion(ITEM, 50L, VERSION, YO))).isEqualTo("COTIZACION_VENCIDA");
        assertThat(g.getPrecioTotal()).isNull();
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void elegirUnaCotizacionDeOtroItemDa404() {
        enCotizacion();
        when(cotizaciones.findByIdAndItemId(77L, ITEM)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.elegirCotizacion(ITEM, 77L, VERSION, YO))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void elegirConUnaVersionVencidaDa409() {
        enCotizacion();
        cotizacion(50L, ITEM, HOY.plusDays(3));
        assertThatThrownBy(() -> service.elegirCotizacion(ITEM, 50L, VERSION - 1, YO))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void elegirEnUnItemQueNoEsMioDa403() {
        enCotizacion();
        cotizacion(50L, ITEM, HOY.plusDays(3));
        assertThatThrownBy(() -> service.elegirCotizacion(ITEM, 50L, VERSION, OTRO))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void cargarElPrecioAManoDesseleccionaLaCotizacion() {
        GestionItem g = enCotizacion();
        Cotizacion c = cotizacion(50L, ITEM, HOY.plusDays(3));
        c.seleccionar();
        g.fijarPrecioTotal(new BigDecimal("900.00"), Moneda.ARS, 50L);

        service.fijarPrecioTotal(ITEM, new PrecioTotalRequest(new BigDecimal("850.50"), Moneda.USD, VERSION), YO);

        assertThat(c.getEstado()).isEqualTo(EstadoCotizacion.CANDIDATA);
        assertThat(g.getCotizacionElegidaId()).isNull();
        assertThat(g.getPrecioTotal()).isEqualByComparingTo("850.50");
        assertThat(g.getMonedaTotal()).isEqualTo(Moneda.USD);
        PrecioTotalActualizado e = evento(PrecioTotalActualizado.class);
        assertThat(e.precioTotal()).isEqualByComparingTo("850.50");
        assertThat(e.cotizacionId()).isNull();
    }

    @Test
    void quitarElPrecioTotalPublicaElEventoConPrecioNulo() {
        GestionItem g = enCotizacion();
        Cotizacion c = cotizacion(50L, ITEM, HOY.plusDays(3));
        c.seleccionar();
        g.fijarPrecioTotal(new BigDecimal("900.00"), Moneda.ARS, 50L);

        service.quitarPrecioTotal(ITEM, VERSION, YO);

        assertThat(g.getPrecioTotal()).isNull();
        assertThat(c.getEstado()).isEqualTo(EstadoCotizacion.CANDIDATA);
        PrecioTotalActualizado e = evento(PrecioTotalActualizado.class);
        assertThat(e.precioTotal()).isNull();
        assertThat(e.moneda()).isNull();
    }

    // ---- T10.4 · rechazar ----

    @Test
    void elEncargadoRechazaSinHaberCotizado() {
        GestionItem g = pendiente();
        service.rechazar(ITEM, "  No hace falta  ", VERSION, OTRO);

        assertThat(g.getEstado()).isEqualTo(EstadoGestion.RECHAZADO);
        assertThat(g.getMotivoRechazo()).isEqualTo("No hace falta");
        assertThat(g.getResueltoEn()).isNotNull();
        ItemResuelto e = evento(ItemResuelto.class);
        assertThat(e.resultado()).isEqualTo("RECHAZADO");
        assertThat(e.rechazadoPor()).isEqualTo("Encargado de compras");
        assertThat(e.motivoRechazo()).isEqualTo("No hace falta");
        assertThat(e.urgencia()).isEqualTo(Urgencia.ALTA);
    }

    @Test
    void rechazarUnItemEnCotizacionDeOtroDa403() {
        enCotizacion();
        assertThatThrownBy(() -> service.rechazar(ITEM, "No", VERSION, OTRO)).isInstanceOf(SinPermisoException.class);
    }

    @Test
    void rechazarUnItemCompradoDa422() {
        GestionItem g = enCotizacion();
        g.marcarAprobada();
        g.fijarPrecioTotal(new BigDecimal("900.00"), Moneda.ARS, null);
        g.registrarCompra(9L, OffsetDateTime.now());

        assertThat(codigo(() -> service.rechazar(ITEM, "No", VERSION, YO))).isEqualTo("GESTION_ESTADO_INVALIDO");
    }

    // ---- T10.7 · comprar ----

    private GestionItem listaParaComprar() {
        GestionItem g = enCotizacion();
        g.marcarAprobada();
        return g;
    }

    @Test
    void comprarSinLaCadenaCompletaDa422() {
        GestionItem g = enCotizacion();
        g.fijarPrecioTotal(new BigDecimal("900.00"), Moneda.ARS, null);
        assertThat(codigo(() -> service.comprar(ITEM, new ComprarRequest(9L, VERSION), YO)))
                .isEqualTo("APROBACION_PENDIENTE");
    }

    @Test
    void comprarSinPrecioTotalDa422() {
        listaParaComprar();
        assertThat(codigo(() -> service.comprar(ITEM, new ComprarRequest(9L, VERSION), YO)))
                .isEqualTo("FALTA_PRECIO_TOTAL");
    }

    @Test
    void comprarSinCotizacionElegidaExigeElProveedor() {
        GestionItem g = listaParaComprar();
        g.fijarPrecioTotal(new BigDecimal("900.00"), Moneda.ARS, null);
        assertThat(codigo(() -> service.comprar(ITEM, new ComprarRequest(null, VERSION), YO)))
                .isEqualTo("PROVEEDOR_REQUERIDO");
    }

    @Test
    void comprarConUnProveedorInactivoDa422() {
        GestionItem g = listaParaComprar();
        g.fijarPrecioTotal(new BigDecimal("900.00"), Moneda.ARS, null);
        when(catalogo.obtenerProveedor(8L)).thenReturn(new ProveedorResumen(8L, "Baja SA", false));
        assertThat(codigo(() -> service.comprar(ITEM, new ComprarRequest(8L, VERSION), YO)))
                .isEqualTo("PROVEEDOR_INACTIVO");
    }

    @Test
    void comprarConLaCotizacionElegidaYaVencidaDa422() {
        GestionItem g = listaParaComprar();
        cotizacion(50L, ITEM, HOY.minusDays(1));
        g.fijarPrecioTotal(new BigDecimal("900.00"), Moneda.ARS, 50L);

        assertThat(codigo(() -> service.comprar(ITEM, new ComprarRequest(null, VERSION), YO)))
                .isEqualTo("COTIZACION_VENCIDA");
        assertThat(g.getEstado()).isEqualTo(EstadoGestion.EN_COTIZACION);
    }

    @Test
    void comprarConUnProveedorDistintoAlDeLaCotizacionDa422() {
        GestionItem g = listaParaComprar();
        cotizacion(50L, ITEM, HOY.plusDays(2));
        g.fijarPrecioTotal(new BigDecimal("900.00"), Moneda.ARS, 50L);

        assertThat(codigo(() -> service.comprar(ITEM, new ComprarRequest(8L, VERSION), YO)))
                .isEqualTo("PROVEEDOR_NO_COINCIDE");
    }

    @Test
    void comprarConUnaVersionVencidaDa409() {
        GestionItem g = listaParaComprar();
        g.fijarPrecioTotal(new BigDecimal("900.00"), Moneda.ARS, null);
        assertThatThrownBy(() -> service.comprar(ITEM, new ComprarRequest(9L, VERSION - 1), YO))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(g.getEstado()).isEqualTo(EstadoGestion.EN_COTIZACION);
    }

    @Test
    void compraFelizConCotizacionDescartaLasDemasYPublicaLosDosEventos() {
        GestionItem g = listaParaComprar();
        Cotizacion elegida = cotizacion(50L, ITEM, HOY.plusDays(2));
        elegida.seleccionar();
        Cotizacion otra = cotizacion(51L, ITEM, HOY.plusDays(2));
        g.fijarPrecioTotal(new BigDecimal("900.00"), Moneda.ARS, 50L);
        when(cotizaciones.findByItemIdOrderByCargadaEnDesc(ITEM)).thenReturn(List.of(elegida, otra));

        service.comprar(ITEM, new ComprarRequest(null, VERSION), YO);

        assertThat(g.getEstado()).isEqualTo(EstadoGestion.COMPRADO);
        assertThat(g.getProveedorCompraId()).isEqualTo(9L);
        assertThat(g.getCompradoEn()).isNotNull();
        assertThat(g.getResueltoEn()).isNotNull();
        assertThat(elegida.getEstado()).isEqualTo(EstadoCotizacion.SELECCIONADA);
        assertThat(otra.getEstado()).isEqualTo(EstadoCotizacion.DESCARTADA);

        CompraRegistrada compra = evento(CompraRegistrada.class);
        assertThat(compra.itemId()).isEqualTo(ITEM);
        assertThat(compra.proveedorId()).isEqualTo(9L);
        assertThat(compra.proveedorRazonSocial()).isEqualTo("Globos SA");
        assertThat(compra.cotizacionId()).isEqualTo(50L);
        assertThat(compra.precioTotal()).isEqualByComparingTo("900.00");
        assertThat(compra.moneda()).isEqualTo(Moneda.ARS);
        assertThat(compra.cantidad()).isEqualByComparingTo("2.00");
        assertThat(compra.tipo()).isEqualTo(TipoItem.PRODUCTO);
        assertThat(compra.encargadoId()).isEqualTo(YO);
        ItemResuelto resuelto = evento(ItemResuelto.class);
        assertThat(resuelto.resultado()).isEqualTo("COMPRADO");
        assertThat(resuelto.proveedorRazonSocial()).isEqualTo("Globos SA");
        verify(eventos, times(2)).publishEvent(any(Object.class));
    }

    @Test
    void compraFelizConPrecioACmanoUsaElProveedorDelPedido() {
        GestionItem g = listaParaComprar();
        g.fijarPrecioTotal(new BigDecimal("700.00"), Moneda.USD, null);
        when(cotizaciones.findByItemIdOrderByCargadaEnDesc(ITEM)).thenReturn(List.of());

        service.comprar(ITEM, new ComprarRequest(9L, VERSION), YO);

        assertThat(g.getEstado()).isEqualTo(EstadoGestion.COMPRADO);
        CompraRegistrada compra = evento(CompraRegistrada.class);
        assertThat(compra.cotizacionId()).isNull();
        assertThat(compra.moneda()).isEqualTo(Moneda.USD);
        assertThat(compra.precioTotal()).isEqualByComparingTo("700.00");
    }

    // ---- T10.8 · editar la compra ----

    private GestionItem comprado() {
        GestionItem g = listaParaComprar();
        g.fijarPrecioTotal(new BigDecimal("900.00"), Moneda.ARS, null);
        g.registrarCompra(9L, OffsetDateTime.now());
        return g;
    }

    @Test
    void editarLaCompraPublicaAnteriorYNuevo() {
        GestionItem g = comprado();
        service.editarCompra(ITEM, new EditarCompraRequest(new BigDecimal("950.00"), new BigDecimal("40.00"), VERSION), YO);

        assertThat(g.getPrecioTotal()).isEqualByComparingTo("950.00");
        assertThat(g.getMontoEnvio()).isEqualByComparingTo("40.00");
        CompraActualizada e = evento(CompraActualizada.class);
        assertThat(e.precioTotalAnterior()).isEqualByComparingTo("900.00");
        assertThat(e.precioTotal()).isEqualByComparingTo("950.00");
        assertThat(e.montoEnvioAnterior()).isNull();
        assertThat(e.montoEnvio()).isEqualByComparingTo("40.00");
        assertThat(e.moneda()).isEqualTo(Moneda.ARS);
        assertThat(e.usuarioId()).isEqualTo(YO);
    }

    @Test
    void cargarSoloElEnvioNoTocaElPrecio() {
        GestionItem g = comprado();
        service.editarCompra(ITEM, new EditarCompraRequest(null, new BigDecimal("40.00"), VERSION), YO);
        assertThat(g.getPrecioTotal()).isEqualByComparingTo("900.00");
        assertThat(g.getMontoEnvio()).isEqualByComparingTo("40.00");
    }

    @Test
    void editarLaCompraDeUnItemNoCompradoDa422() {
        enCotizacion();
        assertThat(codigo(() -> service.editarCompra(ITEM,
                new EditarCompraRequest(new BigDecimal("950.00"), null, VERSION), YO)))
                .isEqualTo("GESTION_ESTADO_INVALIDO");
    }

    @Test
    void editarLaCompraAjenaDa403() {
        comprado();
        assertThatThrownBy(() -> service.editarCompra(ITEM,
                new EditarCompraRequest(new BigDecimal("950.00"), null, VERSION), OTRO))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void editarLaCompraSinNingunImporteDa400() {
        comprado();
        assertThatThrownBy(() -> service.editarCompra(ITEM, new EditarCompraRequest(null, null, VERSION), YO))
                .isInstanceOf(ParametroInvalidoException.class);
    }
}
