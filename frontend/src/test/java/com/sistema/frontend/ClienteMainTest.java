package com.sistema.frontend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ClienteMainTest {

    @Test
    void elPuntoDeEntradaExiste() {
        assertDoesNotThrow(() -> ClienteMain.class.getDeclaredMethod("main", String[].class));
    }

    @Test
    void laClaseNoEsInstanciable() {
        assertTrue(java.lang.reflect.Modifier.isFinal(ClienteMain.class.getModifiers()));
    }
}
