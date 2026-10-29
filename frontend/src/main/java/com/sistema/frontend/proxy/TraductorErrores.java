package com.sistema.frontend.proxy;

import com.sistema.common.excepciones.AccesoDatosException;
import com.sistema.common.excepciones.AutenticacionException;
import com.sistema.common.excepciones.ConfiguracionException;
import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.excepciones.ServicioExternoException;
import com.sistema.common.excepciones.SistemaException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.protocolo.CodigoError;
import com.sistema.common.protocolo.ErrorRemoto;
import java.util.Objects;

/**
 * Convierte el error que viaja por la red en la excepción de dominio que el
 * resto del frontend ya conoce.
 *
 * <p>Así los controladores y las vistas manejan {@code ValidacionException},
 * {@code AutenticacionException}, etc., sin saber que hubo un socket de por
 * medio. El mensaje es el que redactó el backend y está listo para mostrarse.</p>
 */
public final class TraductorErrores {

    private TraductorErrores() {
    }

    /**
     * Traduce un {@link ErrorRemoto} a la excepción de dominio equivalente.
     */
    public static RuntimeException aExcepcion(ErrorRemoto error) {
        Objects.requireNonNull(error, "El error remoto es obligatorio.");
        String mensaje = error.mensaje();
        return switch (error.codigo()) {
            case VALIDACION -> new ValidacionException(mensaje);
            case NO_ENCONTRADO -> new NoEncontradoException(mensaje);
            case CONFLICTO -> new ConflictoException(mensaje);
            case AUTENTICACION -> new AutenticacionException(mensaje);
            case ACCESO_DATOS -> new AccesoDatosException(mensaje);
            case CONFIGURACION -> new ConfiguracionException(mensaje);
            case SERVICIO_EXTERNO -> new ServicioExternoException(mensaje);
            case INTERNO -> new SistemaException(mensaje);
        };
    }

    /**
     * Indica si un código corresponde a un error de dominio conocido.
     * Se ofrece para pruebas y para clasificar mensajes en la interfaz.
     */
    public static boolean esDeDominio(CodigoError codigo) {
        return codigo != null && codigo != CodigoError.INTERNO;
    }
}
