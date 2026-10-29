package com.sistema.common.excepciones;

/**
 * Falta un dato de configuración o tiene un formato inválido.
 */
public class ConfiguracionException extends SistemaException {

    private static final long serialVersionUID = 1L;

    public ConfiguracionException(String mensaje) {
        super(mensaje);
    }

    public ConfiguracionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
