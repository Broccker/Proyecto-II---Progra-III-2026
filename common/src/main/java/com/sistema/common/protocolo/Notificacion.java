package com.sistema.common.protocolo;

import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Reserva;
import java.io.Serializable;
import java.util.Objects;

/**
 * Mensaje asincrónico que el backend envía a los frontends conectados.
 *
 * <p>No es una respuesta a una {@link Solicitud}: llega por el mismo socket
 * cuando ocurre un evento, por ejemplo una reserva creada por otro usuario.</p>
 */
public record Notificacion(TipoNotificacion tipo, CargaNotificacion carga)
        implements Serializable {

    private static final long serialVersionUID = 1L;

    public Notificacion {
        Objects.requireNonNull(tipo, "El tipo de la notificación es obligatorio.");
        Objects.requireNonNull(carga, "La carga de la notificación es obligatoria.");
    }

    /**
     * Crea la notificación de una reserva recién creada.
     */
    public static Notificacion nuevaReserva(Reserva reserva) {
        return new Notificacion(TipoNotificacion.NUEVA_RESERVA,
                CargaNuevaReserva.desde(reserva));
    }

    /**
     * Devuelve la carga comprobando que corresponde al tipo indicado.
     */
    public CargaNuevaReserva comoNuevaReserva() {
        if (tipo != TipoNotificacion.NUEVA_RESERVA
                || !(carga instanceof CargaNuevaReserva nuevaReserva)) {
            throw new ValidacionException(
                    "La notificación no corresponde a una reserva nueva.");
        }
        return nuevaReserva;
    }
}
