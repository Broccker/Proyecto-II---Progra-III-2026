package com.sistema.common.excepciones;

/**
 * Fallo al comunicarse con un servicio externo, por ejemplo el proveedor de IA.
 */
public class ServicioExternoException extends SistemaException {

    private static final long serialVersionUID = 1L;

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
