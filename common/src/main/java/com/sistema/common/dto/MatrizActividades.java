package com.sistema.common.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Matriz semanal de actividades: 24 horas por 7 días.
 *
 * @param lunes  lunes de la semana representada
 * @param horas  horas del día (00:00 a 23:00)
 * @param dias   nombres de los días (lunes a domingo)
 * @param celdas filas por hora; cada fila tiene una celda por día
 */
public record MatrizActividades(LocalDate lunes, List<LocalTime> horas, List<String> dias,
                                List<List<CeldaCalendario>> celdas) implements Serializable {

    public MatrizActividades {
        Objects.requireNonNull(lunes, "La fecha de la semana es obligatoria.");
        horas = List.copyOf(horas);
        dias = List.copyOf(dias);
        celdas = celdas.stream().map(List::copyOf).toList();
    }

    /**
     * Convierte la matriz a texto para mostrarla en una tabla o exportarla.
     * La primera dimensión son las horas y la segunda los días.
     */
    public String[][] aTexto() {
        String[][] matriz = new String[horas.size()][dias.size()];
        for (int fila = 0; fila < horas.size(); fila++) {
            List<CeldaCalendario> celdasFila = celdas.get(fila);
            for (int columna = 0; columna < dias.size(); columna++) {
                matriz[fila][columna] = celdasFila.get(columna).texto();
            }
        }
        return matriz;
    }
}
