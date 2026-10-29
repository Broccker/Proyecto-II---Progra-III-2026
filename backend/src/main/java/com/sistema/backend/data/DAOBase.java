package com.sistema.backend.data;

import com.sistema.common.excepciones.AccesoDatosException;
import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.ValidacionException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base común de los DAO: obtiene conexiones del pool, centraliza las
 * transacciones y traduce {@link SQLException} a excepciones de dominio.
 *
 * <p>Los DAO usan siempre {@link java.sql.PreparedStatement} y
 * {@code try-with-resources}; ninguna consulta se arma concatenando valores.</p>
 */
abstract class DAOBase {

    /** Trabajo que se ejecuta con una conexión abierta. */
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

    /**
     * Ejecuta una consulta o una operación sin transacción explícita.
     */
    protected <T> T enConsulta(String operacion, TrabajoBD<T> trabajo) {
        try (Connection conexion = fuente.conectar()) {
            return trabajo.ejecutar(conexion);
        } catch (SQLException e) {
            throw traducir(operacion, e);
        }
    }

    /**
     * Ejecuta varias sentencias como una sola unidad atómica: si algo falla,
     * se revierte todo. Se usa, por ejemplo, al crear una reserva junto con
     * su detalle de recursos.
     */
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
                    // La conexión se devuelve al pool de todos modos.
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
            // Si el rollback falla, se informa el error original.
        }
    }

    /**
     * Traduce los errores de MySQL más frecuentes a excepciones con mensajes
     * en español, para que la lógica y el frontend no dependan del driver.
     */
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
