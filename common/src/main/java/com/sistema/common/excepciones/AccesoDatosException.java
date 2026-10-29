package com.sistema.common.excepciones;

/**
 * Fallo de infraestructura al comunicarse con la base de datos
 * (servidor caído, credenciales incorrectas, error de SQL no clasificado).
 */
public class AccesoDatosException extends SistemaException {

    private static final long serialVersionUID = 1L;

    public AccesoDatosException(String mensaje) {
        super(mensaje);
    }

    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
