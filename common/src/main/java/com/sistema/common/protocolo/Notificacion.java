package com.sistema.common.protocolo;

import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Reserva;
import java.io.Serializable;
import java.util.Objects;


public record Notificacion(TipoNotificacion tipo, CargaNotificacion carga)
        implements Serializable {

    private static final long serialVersionUID = 1L;

    public Notificacion {
        Objects.requireNonNull(tipo, "El tipo de la notificación es obligatorio.");
        Objects.requireNonNull(carga, "La carga de la notificación es obligatoria.");
    }


    public static Notificacion nuevaReserva(Reserva reserva) {
        return new Notificacion(TipoNotificacion.NUEVA_RESERVA,
                CargaNuevaReserva.desde(reserva));
    }


    public CargaNuevaReserva comoNuevaReserva() {
        if (tipo != TipoNotificacion.NUEVA_RESERVA
                || !(carga instanceof CargaNuevaReserva nuevaReserva)) {
            throw new ValidacionException(
                    "La notificación no corresponde a una reserva nueva.");
        }
        return nuevaReserva;
    }
}
