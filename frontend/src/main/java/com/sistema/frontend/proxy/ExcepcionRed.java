package com.sistema.frontend.proxy;

import com.sistema.common.protocolo.Operacion;
import java.util.Objects;


public class ExcepcionRed extends RuntimeException {

    private static final long serialVersionUID = 1L;


    public enum Motivo {

        SIN_CONEXION,

        CONEXION_PERDIDA,

        TIEMPO_AGOTADO,

        CERRADO,

        INTERRUMPIDA,

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
