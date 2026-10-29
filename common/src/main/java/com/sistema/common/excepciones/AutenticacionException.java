package com.sistema.common.excepciones;

/**
 * Credenciales incorrectas o usuario no autenticado.
 */
public class AutenticacionException extends SistemaException {

    private static final long serialVersionUID = 1L;

    public AutenticacionException(String mensaje) {
        super(mensaje);
    }
}
