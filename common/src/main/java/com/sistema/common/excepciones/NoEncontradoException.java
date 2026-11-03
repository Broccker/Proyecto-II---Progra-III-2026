package com.sistema.common.excepciones;


public class NoEncontradoException extends SistemaException {

    private static final long serialVersionUID = 1L;

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
