package com.sistema.backend.logic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.excepciones.ValidacionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


class ValidacionesTest {

    @Test
    @DisplayName("Recorta un texto válido y devuelve su valor")
    void aceptaYRecortaTexto() {
        assertEquals("Sala de reuniones",
                Validaciones.texto("  Sala de reuniones  ", "La descripción", 100));
    }

    @Test
    @DisplayName("Rechaza un texto nulo o en blanco con un mensaje claro")
    void rechazaTextoEnBlanco() {
        ValidacionException nulo = assertThrows(ValidacionException.class,
                () -> Validaciones.texto(null, "La actividad", 150));
        ValidacionException vacio = assertThrows(ValidacionException.class,
                () -> Validaciones.texto("   ", "La actividad", 150));

        assertTrue(nulo.getMessage().contains("La actividad"));
        assertTrue(nulo.getMessage().contains("blanco"));
        assertEquals(nulo.getMessage(), vacio.getMessage());
    }

    @Test
    @DisplayName("Rechaza un texto que supera el máximo de caracteres")
    void rechazaTextoDemasiadoLargo() {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> Validaciones.texto("x".repeat(101), "El nombre", 100));

        assertTrue(error.getMessage().contains("100"));
        assertTrue(error.getMessage().contains("El nombre"));
    }

    @Test
    @DisplayName("La comprobación de longitud tolera nulos y acepta el límite exacto")
    void validaLongitud() {
        assertDoesNotThrow(() -> Validaciones.longitud(null, 10, "El campo"));
        assertDoesNotThrow(() -> Validaciones.longitud("x".repeat(10), 10, "El campo"));
        assertThrows(ValidacionException.class,
                () -> Validaciones.longitud("x".repeat(11), 10, "El campo"));
    }

    @Test
    @DisplayName("La clave conserva los espacios y admite hasta 72 bytes")
    void validaClave() {
        assertDoesNotThrow(() -> Validaciones.clave(" con espacios ", "La clave"));
        assertDoesNotThrow(() -> Validaciones.clave("a".repeat(72), "La clave"));

        assertThrows(ValidacionException.class,
                () -> Validaciones.clave("   ", "La clave"));
        assertThrows(ValidacionException.class,
                () -> Validaciones.clave("a".repeat(73), "La clave"));
    }

    @Test
    @DisplayName("El límite de la clave se mide en bytes, no en caracteres")
    void mideLaClaveEnBytes() {

        assertThrows(ValidacionException.class,
                () -> Validaciones.clave("á".repeat(37), "La clave"));
    }
}
