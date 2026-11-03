package com.sistema.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ServidorMainTest {

    @Test
    void elPuntoDeEntradaExiste() {
        assertDoesNotThrow(() -> ServidorMain.class.getDeclaredMethod("main", String[].class));
    }

    @Test
    void laClaseNoEsInstanciable() {
        assertTrue(java.lang.reflect.Modifier.isFinal(ServidorMain.class.getModifiers()));
    }
}
