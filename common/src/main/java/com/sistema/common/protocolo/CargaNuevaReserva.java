package com.sistema.common.protocolo;

import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Datos de una reserva recién creada que se envían a los demás frontends.
 *
 * @param reservaId  identificador de la reserva
 * @param encargado  nombre del funcionario que la creó
 * @param actividad  actividad reservada
 * @param fecha      día de la reserva
 * @param horaInicio hora desde
 * @param horaFin    hora hasta
 * @param recursos   recursos asignados
 */
public record CargaNuevaReserva(String reservaId, String encargado, String actividad,
                                LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                                List<Recurso> recursos) implements CargaNotificacion {

    private static final long serialVersionUID = 1L;

    public CargaNuevaReserva {
        if (reservaId == null || reservaId.isBlank()) {
            throw new ValidacionException(
                    "El identificador de la reserva de la notificación no puede estar vacío.");
        }
        if (encargado == null || encargado.isBlank()) {
            throw new ValidacionException(
                    "El encargado de la reserva de la notificación no puede estar vacío.");
        }
        if (actividad == null || actividad.isBlank()) {
            throw new ValidacionException(
                    "La actividad de la reserva de la notificación no puede estar vacía.");
        }
        Objects.requireNonNull(fecha, "La fecha de la reserva es obligatoria.");
        Objects.requireNonNull(horaInicio, "La hora de inicio es obligatoria.");
        Objects.requireNonNull(horaFin, "La hora de fin es obligatoria.");
        if (!horaFin.isAfter(horaInicio)) {
            throw new ValidacionException(
                    "La hora de fin de la notificación debe ser posterior a la hora de inicio.");
        }
        reservaId = reservaId.trim();
        encargado = encargado.trim();
        actividad = actividad.trim();
        recursos = List.copyOf(recursos);
    }

    /**
     * Construye la carga a partir de una reserva del dominio.
     */
    public static CargaNuevaReserva desde(Reserva reserva) {
        Objects.requireNonNull(reserva, "La reserva es obligatoria.");
        return new CargaNuevaReserva(reserva.getId(),
                reserva.getFuncionario().getNombre(), reserva.getActividad(),
                reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin(),
                reserva.getRecursos());
    }
}
