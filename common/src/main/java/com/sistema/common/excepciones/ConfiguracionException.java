package com.sistema.common.excepciones;


public class ConfiguracionException extends SistemaException {

    private static final long serialVersionUID = 1L;

    public ConfiguracionException(String mensaje) {
        super(mensaje);
    }

    public ConfiguracionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
