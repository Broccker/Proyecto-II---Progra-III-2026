package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sistema.common.excepciones.AutenticacionException;
import com.sistema.common.excepciones.ConflictoException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErroresTest {

    @Test
    @DisplayName("Un error de dominio muestra su mensaje")
    void usaElMensajeDeDominio() {
        assertEquals("Credenciales incorrectas.",
                Errores.mensaje(new AutenticacionException("Credenciales incorrectas.")));
    }

    @Test
    @DisplayName("Desenvuelve la causa de un fallo asíncrono")
    void desenvuelveLaCausa() {
        ExecutionException error = new ExecutionException(
                new ConflictoException("No hay recursos disponibles."));

        assertEquals("No hay recursos disponibles.", Errores.mensaje(error));
    }

    @Test
    @DisplayName("Un error inesperado no expone detalles internos")
    void resumenLosErroresInesperados() {
        assertEquals("Ocurrió un error inesperado. (IllegalStateException)",
                Errores.mensaje(new IllegalStateException()));
    }

    @Test
    @DisplayName("Un error nulo devuelve el mensaje genérico")
    void errorNulo() {
        assertEquals("Ocurrió un error inesperado.", Errores.mensaje(null));
    }
}
