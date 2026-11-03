package com.sistema.common.excepciones;


public class SistemaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SistemaException(String mensaje) {
        super(mensaje);
    }

    public SistemaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
