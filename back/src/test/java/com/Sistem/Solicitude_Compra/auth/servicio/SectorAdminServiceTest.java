package com.Sistem.Solicitude_Compra.auth.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.auth.dominio.Sector;
import com.Sistem.Solicitude_Compra.auth.repositorio.SectorRepository;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SectorAdminServiceTest {

    SectorRepository sectores = mock(SectorRepository.class);
    SectorAdminService service = new SectorAdminService(sectores);

    @Test
    void creaElSector() {
        when(sectores.existsByNombreIgnoreCase("Logística")).thenReturn(false);
        when(sectores.save(any(Sector.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("Logística", service.crear(" Logística ").getNombre());
    }

    @Test
    void nombreDuplicadoSinDistinguirMayusculasDa409() {
        when(sectores.existsByNombreIgnoreCase("compras")).thenReturn(true);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> service.crear("compras"));

        assertEquals("SECTOR_DUPLICADO", ex.getCodigo());
        verify(sectores, never()).save(any());
    }

    @Test
    void renombrarActualizaElNombre() {
        Sector s = sector(1L, "Compras");
        when(sectores.findById(1L)).thenReturn(Optional.of(s));
        when(sectores.existsByNombreIgnoreCase("Abastecimiento")).thenReturn(false);

        assertEquals("Abastecimiento", service.renombrar(1L, "Abastecimiento").getNombre());
    }

    @Test
    void renombrarAlNombreDeOtroSectorDa409() {
        when(sectores.findById(1L)).thenReturn(Optional.of(sector(1L, "Compras")));
        when(sectores.existsByNombreIgnoreCase("Sistemas")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.renombrar(1L, "Sistemas"));
    }

    @Test
    void cambiarSoloLasMayusculasDelPropioNombreEsValido() {
        Sector s = sector(1L, "compras");
        when(sectores.findById(1L)).thenReturn(Optional.of(s));
        when(sectores.existsByNombreIgnoreCase("Compras")).thenReturn(true); // se encuentra a si mismo

        assertEquals("Compras", service.renombrar(1L, "Compras").getNombre());
    }

    @Test
    void renombrarUnoInexistenteDa404() {
        when(sectores.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> service.renombrar(9L, "X"));
    }

    private static Sector sector(Long id, String nombre) {
        Sector s = new Sector();
        s.setId(id);
        s.setNombre(nombre);
        return s;
    }
}
