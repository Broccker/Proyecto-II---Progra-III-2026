package com.sistema.backend.data;

import com.sistema.common.excepciones.AccesoDatosException;
import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.ValidacionException;
import java.sql.Connection;
import java.sql.SQLException;


abstract class DAOBase {


    @FunctionalInterface
    protected interface TrabajoBD<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    private static final int ERROR_DUPLICADO = 1062;
    private static final int ERROR_CLAVE_FORANEA_PADRE = 1451;
    private static final int ERROR_CLAVE_FORANEA_HIJA = 1452;
    private static final int ERROR_RESTRICCION_CHECK = 3819;

    protected final FuenteDatos fuente;

    protected DAOBase(FuenteDatos fuente) {
        this.fuente = fuente;
    }


    protected <T> T enConsulta(String operacion, TrabajoBD<T> trabajo) {
        try (Connection conexion = fuente.conectar()) {
            return trabajo.ejecutar(conexion);
        } catch (SQLException e) {
            throw traducir(operacion, e);
        }
    }


    protected <T> T enTransaccion(String operacion, TrabajoBD<T> trabajo) {
        try (Connection conexion = fuente.conectar()) {
            boolean autoCommit = conexion.getAutoCommit();
            try {
                conexion.setAutoCommit(false);
                T resultado = trabajo.ejecutar(conexion);
                conexion.commit();
                return resultado;
            } catch (SQLException e) {
                revertir(conexion);
                throw traducir(operacion, e);
            } catch (RuntimeException e) {
                revertir(conexion);
                throw e;
            } finally {
                try {
                    conexion.setAutoCommit(autoCommit);
                } catch (SQLException ignored) {

                }
            }
        } catch (SQLException e) {
            throw traducir(operacion, e);
        }
    }

    private static void revertir(Connection conexion) {
        try {
            conexion.rollback();
        } catch (SQLException ignored) {

        }
    }


    private static RuntimeException traducir(String operacion, SQLException e) {
        return switch (e.getErrorCode()) {
            case ERROR_DUPLICADO -> new ConflictoException(
                    "Ya existe un registro con ese identificador o esa descripción.", e);
            case ERROR_CLAVE_FORANEA_PADRE -> new ConflictoException(
                    "No se puede eliminar o modificar: el registro tiene datos relacionados.", e);
            case ERROR_CLAVE_FORANEA_HIJA -> new ConflictoException(
                    "El dato relacionado indicado no existe.", e);
            case ERROR_RESTRICCION_CHECK -> new ValidacionException(
                    "Los datos no cumplen una restricción de la base de datos.", e);
            default -> new AccesoDatosException(
                    "Error de base de datos al " + operacion + ": " + e.getMessage(), e);
        };
    }
}
