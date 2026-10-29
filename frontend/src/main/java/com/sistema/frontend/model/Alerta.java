package com.sistema.frontend.model;

import com.sistema.common.modelo.Recurso;
import com.sistema.common.protocolo.CargaNuevaReserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Alerta local de una reserva creada por otro usuario.
 *
 * <p>Es un valor inmutable: se construye a partir de la carga de la
 * {@code Notificacion} y no se persiste. Vive solo en memoria mientras dure la
 * sesión.</p>
 */
public record Alerta(String reservaId, String encargado, String actividad,
                     LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                     List<Recurso> recursos) {

    public Alerta {
        Objects.requireNonNull(reservaId, "El identificador de la reserva es obligatorio.");
        Objects.requireNonNull(encargado, "El encargado es obligatorio.");
        Objects.requireNonNull(actividad, "La actividad es obligatoria.");
        Objects.requireNonNull(fecha, "La fecha es obligatoria.");
        Objects.requireNonNull(horaInicio, "La hora de inicio es obligatoria.");
        Objects.requireNonNull(horaFin, "La hora de fin es obligatoria.");
        recursos = List.copyOf(recursos);
    }

    /** Construye la alerta con los datos que envió el backend. */
    public static Alerta desde(CargaNuevaReserva carga) {
        Objects.requireNonNull(carga, "La carga de la notificación es obligatoria.");
        return new Alerta(carga.reservaId(), carga.encargado(), carga.actividad(),
                carga.fecha(), carga.horaInicio(), carga.horaFin(), carga.recursos());
    }

    /** Texto de la columna Actividad: {@code Encargado - Nombre actividad}. */
    public String textoActividad() {
        return encargado + " - " + actividad;
    }

    /** Descripciones de los recursos separadas por coma. */
    public String textoRecursos() {
        return recursos.stream()
                .map(Recurso::getDescripcion)
                .reduce((primero, segundo) -> primero + ", " + segundo)
                .orElse("Sin recursos");
    }
}
