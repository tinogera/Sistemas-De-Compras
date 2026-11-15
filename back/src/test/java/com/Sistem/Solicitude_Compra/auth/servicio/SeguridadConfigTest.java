package com.Sistem.Solicitude_Compra.auth.servicio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SeguridadConfigTest {

    SeguridadConfig config = new SeguridadConfig();

    @Test
    void rechazaUnSecretoJwtCorto() {
        assertThrows(IllegalStateException.class, () -> config.claveJwt("x".repeat(47)));
    }

    @Test
    void aceptaUnSecretoJwtDe48Caracteres() {
        assertDoesNotThrow(() -> config.claveJwt("x".repeat(48)));
    }
}
