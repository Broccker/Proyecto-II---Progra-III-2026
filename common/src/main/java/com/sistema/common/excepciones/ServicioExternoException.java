package com.sistema.common.excepciones;


public class ServicioExternoException extends SistemaException {

    private static final long serialVersionUID = 1L;

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
