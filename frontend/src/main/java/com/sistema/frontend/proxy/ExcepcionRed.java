package com.sistema.frontend.proxy;

import com.sistema.common.protocolo.Operacion;
import java.util.Objects;

/**
 * Error de comunicación con el backend.
 *
 * <p>No es un error de dominio: representa que el servidor no está
 * disponible, que la conexión se perdió o que una respuesta no llegó a
 * tiempo. El {@link Motivo} permite que la interfaz decida si conviene
 * reintentar o pedir al usuario que revise la red.</p>
 */
public class ExcepcionRed extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Causa clasificada del fallo de comunicación. */
    public enum Motivo {
        /** No se pudo abrir la conexión con el backend. */
        SIN_CONEXION,
        /** La conexión existente se cerró de forma inesperada. */
        CONEXION_PERDIDA,
        /** El backend no respondió dentro del tiempo configurado. */
        TIEMPO_AGOTADO,
        /** El proxy ya fue cerrado por el frontend. */
        CERRADO,
        /** El hilo que esperaba la respuesta fue interrumpido. */
        INTERRUMPIDA,
        /** El servidor devolvió un mensaje con un tipo inesperado. */
        RESPUESTA_INVALIDA
    }

    private final Motivo motivo;

    public ExcepcionRed(Motivo motivo, String mensaje) {
        super(mensaje);
        this.motivo = Objects.requireNonNull(motivo, "El motivo es obligatorio.");
    }

    public ExcepcionRed(Motivo motivo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.motivo = Objects.requireNonNull(motivo, "El motivo es obligatorio.");
    }

    public Motivo motivo() {
        return motivo;
    }

    static ExcepcionRed sinConexion(String host, int puerto, Throwable causa) {
        return new ExcepcionRed(Motivo.SIN_CONEXION,
                "No se pudo conectar con el backend en " + host + ":" + puerto
                        + ". Verifique que el servidor esté en ejecución.", causa);
    }

    static ExcepcionRed conexionPerdida(Throwable causa) {
        return new ExcepcionRed(Motivo.CONEXION_PERDIDA,
                "Se perdió la conexión con el backend. Vuelva a iniciar sesión.", causa);
    }

    static ExcepcionRed tiempoAgotado(Operacion operacion, int milisegundos) {
        return new ExcepcionRed(Motivo.TIEMPO_AGOTADO,
                "El backend no respondió a " + operacion + " en " + milisegundos + " ms.");
    }

    static ExcepcionRed cerrado() {
        return new ExcepcionRed(Motivo.CERRADO,
                "La conexión con el backend ya fue cerrada.");
    }

    static ExcepcionRed interrumpida(Throwable causa) {
        return new ExcepcionRed(Motivo.INTERRUMPIDA,
                "Se interrumpió la espera de la respuesta del backend.", causa);
    }

    static ExcepcionRed respuestaInvalida(String detalle) {
        return new ExcepcionRed(Motivo.RESPUESTA_INVALIDA,
                "El backend devolvió una respuesta inesperada. " + detalle);
    }
}
