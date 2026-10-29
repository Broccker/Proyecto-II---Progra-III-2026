package com.sistema.common.dto;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Cantidad de actividades (reservas activas) de una semana.
 *
 * @param lunes    fecha del lunes que identifica la semana
 * @param cantidad número de reservas activas de esa semana
 */
public record ConteoSemana(LocalDate lunes, long cantidad) implements Serializable {

    public ConteoSemana {
        if (lunes == null) {
            throw new IllegalArgumentException("La fecha de la semana no puede ser nula.");
        }
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        }
    }
}
