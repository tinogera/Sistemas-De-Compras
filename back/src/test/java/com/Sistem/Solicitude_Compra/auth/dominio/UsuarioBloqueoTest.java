package com.Sistem.Solicitude_Compra.auth.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class UsuarioBloqueoTest {

    Instant ahora = Instant.parse("2026-10-25T12:00:00Z");

    @Test
    void conCuatroFallosTodaviaNoBloquea() {
        Usuario u = new Usuario();
        for (int i = 0; i < 4; i++) {
            u.registrarFallo(ahora);
        }

        assertFalse(u.estaBloqueado(ahora));
    }

    @Test
    void elQuintoFalloBloqueaDiezMinutos() {
        Usuario u = fallos(5);

        assertTrue(u.estaBloqueado(ahora.plus(Duration.ofMinutes(9))));
        assertFalse(u.estaBloqueado(ahora.plus(Duration.ofMinutes(10))));
    }

    @Test
    void elDecimoFalloBloqueaVeinteMinutos() {
        Usuario u = fallos(10);

        assertTrue(u.estaBloqueado(ahora.plus(Duration.ofMinutes(19))));
        assertFalse(u.estaBloqueado(ahora.plus(Duration.ofMinutes(20))));
    }

    @Test
    void loginExitosoLimpiaElContador() {
        Usuario u = fallos(5);

        u.registrarExito();

        assertEquals(0, u.getIntentosFallidos());
        assertNull(u.getBloqueadoHasta());
    }

    private Usuario fallos(int cantidad) {
        Usuario u = new Usuario();
        for (int i = 0; i < cantidad; i++) {
            u.registrarFallo(ahora);
        }
        return u;
    }
}
