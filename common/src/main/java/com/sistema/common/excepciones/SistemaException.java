package com.sistema.common.excepciones;

/**
 * Excepción base del sistema. Todas las excepciones de dominio heredan de aquí,
 * de modo que el frontend puede distinguirlas de un fallo inesperado.
 */
public class SistemaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SistemaException(String mensaje) {
        super(mensaje);
    }

    public SistemaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
