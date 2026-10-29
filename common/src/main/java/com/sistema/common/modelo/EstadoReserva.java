package com.sistema.common.modelo;

/**
 * Estados posibles de una reserva.
 *
 * <p>Las reservas canceladas se conservan por historial, pero no se toman en
 * cuenta para la disponibilidad ni para las estadísticas.</p>
 */
public enum EstadoReserva {
    ACTIVA,
    CANCELADA
}
