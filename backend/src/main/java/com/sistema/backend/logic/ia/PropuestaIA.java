package com.sistema.backend.logic.ia;

import com.sistema.common.excepciones.ValidacionException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Datos crudos que devuelve el proveedor de IA, antes de resolver los nombres
 * de categoría contra las que existen en el sistema.
 */
public record PropuestaIA(String actividad, LocalDate fecha, LocalTime horaInicio,
                          LocalTime horaFin, List<String> categorias) {

    public PropuestaIA {
        if (actividad == null || actividad.isBlank()) {
            throw new ValidacionException("La respuesta de la IA no incluye la actividad.");
        }
        Objects.requireNonNull(fecha, "La respuesta de la IA no incluye una fecha válida.");
        Objects.requireNonNull(horaInicio, "La respuesta de la IA no incluye la hora de inicio.");
        Objects.requireNonNull(horaFin, "La respuesta de la IA no incluye la hora de fin.");
        if (!horaFin.isAfter(horaInicio)) {
            throw new ValidacionException(
                    "La hora de fin sugerida debe ser posterior a la hora de inicio.");
        }
        categorias = categorias == null ? List.of() : categorias.stream()
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
