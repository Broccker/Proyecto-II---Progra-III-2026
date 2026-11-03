package com.sistema.common.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;


public record MatrizActividades(LocalDate lunes, List<LocalTime> horas, List<String> dias,
                                List<List<CeldaCalendario>> celdas) implements Serializable {

    public MatrizActividades {
        Objects.requireNonNull(lunes, "La fecha de la semana es obligatoria.");
        horas = List.copyOf(horas);
        dias = List.copyOf(dias);
        celdas = celdas.stream().map(List::copyOf).toList();
    }


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
