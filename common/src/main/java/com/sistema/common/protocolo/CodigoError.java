package com.sistema.common.protocolo;

import com.sistema.common.excepciones.AccesoDatosException;
import com.sistema.common.excepciones.AutenticacionException;
import com.sistema.common.excepciones.ConfiguracionException;
import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.excepciones.ServicioExternoException;
import com.sistema.common.excepciones.ValidacionException;


public enum CodigoError {

    VALIDACION,
    NO_ENCONTRADO,
    CONFLICTO,
    AUTENTICACION,
    ACCESO_DATOS,
    CONFIGURACION,
    SERVICIO_EXTERNO,
    INTERNO;


    public static CodigoError de(Throwable error) {
        if (error instanceof ValidacionException) {
            return VALIDACION;
        }
        if (error instanceof NoEncontradoException) {
            return NO_ENCONTRADO;
        }
        if (error instanceof ConflictoException) {
            return CONFLICTO;
        }
        if (error instanceof AutenticacionException) {
            return AUTENTICACION;
        }
        if (error instanceof AccesoDatosException) {
            return ACCESO_DATOS;
        }
        if (error instanceof ConfiguracionException) {
            return CONFIGURACION;
        }
        if (error instanceof ServicioExternoException) {
            return SERVICIO_EXTERNO;
        }
        return INTERNO;
    }
}
