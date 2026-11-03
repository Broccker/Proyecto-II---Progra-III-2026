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


public final class TraductorErrores {

    private TraductorErrores() {
    }


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


    public static boolean esDeDominio(CodigoError codigo) {
        return codigo != null && codigo != CodigoError.INTERNO;
    }
}
