package com.sistema.common.excepciones;

/**
 * Los datos enviados por el usuario no cumplen una regla de validación.
 */
public class ValidacionException extends SistemaException {

    private static final long serialVersionUID = 1L;

    public ValidacionException(String mensaje) {
        super(mensaje);
    }

    public ValidacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
