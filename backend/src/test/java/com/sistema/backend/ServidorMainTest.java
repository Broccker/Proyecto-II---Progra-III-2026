package com.sistema.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.BindException;
import java.net.ConnectException;


class ServidorMainTest {

    @Test
    void elPuntoDeEntradaExiste() {
        assertDoesNotThrow(() -> ServidorMain.class.getDeclaredMethod("main", String[].class));
    }

    @Test
    void laClaseNoEsInstanciable() {
        assertTrue(java.lang.reflect.Modifier.isFinal(ServidorMain.class.getModifiers()));
    }

    @Test
    void explicaElFalloDeConexionConMySQL() {
        String mensaje = ServidorMain.mensajeDeInicio(
                new RuntimeException(new ConnectException("Connection refused")));

        assertTrue(mensaje.contains("MySQL"));
        assertTrue(mensaje.contains("backend.properties"));
    }

    @Test
    void explicaElPuertoOcupado() {
        String mensaje = ServidorMain.mensajeDeInicio(
                new RuntimeException(new BindException("Address already in use")));

        assertTrue(mensaje.contains("puerto"));
        assertTrue(mensaje.contains("servidor.puerto"));
    }
}
