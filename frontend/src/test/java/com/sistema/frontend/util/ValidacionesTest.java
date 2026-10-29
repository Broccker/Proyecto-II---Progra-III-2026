package com.sistema.frontend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidacionesTest {

    @Test
    @DisplayName("Recorta y acepta un texto dentro del límite")
    void aceptaTextoValido() {
        assertEquals("Sala de reuniones",
                Validaciones.texto("  Sala de reuniones  ", "La descripción", 100));
    }

    @Test
    @DisplayName("Rechaza un texto vacío con un mensaje claro")
    void rechazaTextoVacio() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Validaciones.texto("   ", "La actividad", 150));
        assertTrue(error.getMessage().contains("La actividad"));
        assertTrue(error.getMessage().contains("blanco"));
    }

    @Test
    @DisplayName("Rechaza un texto que supera el máximo")
    void rechazaTextoDemasiadoLargo() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Validaciones.texto("x".repeat(101), "La descripción", 100));
        assertTrue(error.getMessage().contains("100"));
    }

    @Test
    @DisplayName("El identificador admite hasta veinte caracteres")
    void validaIdentificador() {
        assertEquals("CAT-000001", Validaciones.identificador(" CAT-000001 ",
                "El identificador"));
        assertThrows(IllegalArgumentException.class,
                () -> Validaciones.identificador("X".repeat(21), "El identificador"));
    }

    @Test
    @DisplayName("El teléfono exige entre siete y quince dígitos")
    void validaTelefono() {
        assertEquals("88881111", Validaciones.telefono(" 88881111 "));
        assertEquals("+50688881111", Validaciones.telefono("+50688881111"));

        assertThrows(IllegalArgumentException.class,
                () -> Validaciones.telefono("123456"));
        assertThrows(IllegalArgumentException.class,
                () -> Validaciones.telefono("8888-1111"));
        assertThrows(IllegalArgumentException.class,
                () -> Validaciones.telefono("+"));
    }

    @Test
    @DisplayName("La clave conserva los espacios y limita los bytes de BCrypt")
    void validaClave() {
        assertEquals(" con espacios ", Validaciones.clave(" con espacios ", "La clave"));
        assertThrows(IllegalArgumentException.class,
                () -> Validaciones.clave("   ", "La clave"));
        assertThrows(IllegalArgumentException.class,
                () -> Validaciones.clave("á".repeat(37), "La clave"));
    }

    @Test
    @DisplayName("Los campos opcionales vacíos se devuelven como cadena vacía")
    void validaOpcionales() {
        assertEquals("", Validaciones.opcional(null, "El identificador", 20));
        assertEquals("", Validaciones.opcional("  ", "El identificador", 20));
        assertEquals("CAT-1", Validaciones.opcional(" CAT-1 ", "El identificador", 20));
        assertThrows(IllegalArgumentException.class,
                () -> Validaciones.opcional("X".repeat(21), "El identificador", 20));
    }
}
