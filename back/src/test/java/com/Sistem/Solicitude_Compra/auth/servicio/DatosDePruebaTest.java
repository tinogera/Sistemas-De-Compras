package com.Sistem.Solicitude_Compra.auth.servicio;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.auth.dominio.Sector;
import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.auth.repositorio.SectorRepository;
import com.Sistem.Solicitude_Compra.auth.repositorio.UsuarioRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class DatosDePruebaTest {

    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    SectorRepository sectores = mock(SectorRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test
    void conLaTablaVaciaCreaCincoSectoresYCincoUsuarios() throws Exception {
        when(usuarios.count()).thenReturn(0L);
        when(sectores.findAll()).thenReturn(List.of());
        when(sectores.save(any(Sector.class))).thenAnswer(i -> i.getArgument(0));
        when(encoder.encode("clave1234")).thenReturn("hash");

        new DatosDePrueba(usuarios, sectores, encoder, "clave1234").run();

        verify(sectores, times(5)).save(any(Sector.class));
        verify(usuarios, times(5)).save(any(Usuario.class));
    }

    @Test
    void siYaHayUsuariosNoHaceNada() throws Exception {
        when(usuarios.count()).thenReturn(3L);

        new DatosDePrueba(usuarios, sectores, encoder, "clave1234").run();

        verify(usuarios, never()).save(any());
        verify(sectores, never()).save(any());
    }

    @Test
    void sinClaveConfiguradaFallaAlArrancar() {
        when(usuarios.count()).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> new DatosDePrueba(usuarios, sectores, encoder, "").run());
    }
}
