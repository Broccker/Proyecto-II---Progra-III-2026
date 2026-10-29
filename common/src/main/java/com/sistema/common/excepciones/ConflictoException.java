package com.sistema.common.excepciones;

/**
 * La operación choca con un dato existente o con una regla de integridad:
 * identificador duplicado, referencia inexistente, registro en uso, etc.
 */
public class ConflictoException extends SistemaException {

    private static final long serialVersionUID = 1L;

    public ConflictoException(String mensaje) {
        super(mensaje);
    }

    public ConflictoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
