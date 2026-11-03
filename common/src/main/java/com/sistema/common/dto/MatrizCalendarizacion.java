package com.sistema.common.dto;

import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Recurso;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;


public record MatrizCalendarizacion(LocalDate fecha, CategoriaRecurso categoria,
                                    List<Recurso> recursos, List<LocalTime> horas,
                                    List<List<CeldaCalendario>> celdas) implements Serializable {

    public MatrizCalendarizacion {
        Objects.requireNonNull(fecha, "La fecha es obligatoria.");
        Objects.requireNonNull(categoria, "La categoría es obligatoria.");
        recursos = List.copyOf(recursos);
        horas = List.copyOf(horas);
        celdas = celdas.stream().map(List::copyOf).toList();
    }


    public String[][] aTexto() {
        String[][] matriz = new String[horas.size()][recursos.size()];
        for (int fila = 0; fila < horas.size(); fila++) {
            List<CeldaCalendario> celdasFila = celdas.get(fila);
            for (int columna = 0; columna < recursos.size(); columna++) {
                matriz[fila][columna] = celdasFila.get(columna).texto();
            }
        }
        return matriz;
    }
}
