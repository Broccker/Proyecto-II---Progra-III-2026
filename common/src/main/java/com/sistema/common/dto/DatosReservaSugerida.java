package com.sistema.common.dto;

import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.CategoriaRecurso;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;


public record DatosReservaSugerida(String actividad, LocalDate fecha, LocalTime horaInicio,
                                   LocalTime horaFin, List<CategoriaRecurso> categorias,
                                   List<String> categoriasNoEncontradas) implements Serializable {

    public DatosReservaSugerida {
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
        categorias = categorias == null ? List.of() : List.copyOf(categorias);
        categoriasNoEncontradas = categoriasNoEncontradas == null
                ? List.of() : List.copyOf(categoriasNoEncontradas);
    }
}
