package com.sistema.frontend.model;

import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import java.util.List;

/**
 * Estado de la vista de estadísticas.
 *
 * <p>Guarda los dos resultados que entrega el backend ya agregados en SQL: las
 * reservas por categoría y las actividades por semana. La vista solo los
 * pinta; los filtros de fecha y las consultas los maneja el controlador.</p>
 */
public final class ModeloEstadisticas {

    private List<ConteoCategoria> categorias = List.of();
    private List<ConteoSemana> semanas = List.of();

    /** Reservas por categoría del último rango consultado. */
    public List<ConteoCategoria> getCategorias() {
        return categorias;
    }

    public void establecerCategorias(List<ConteoCategoria> categorias) {
        this.categorias = categorias == null ? List.of() : List.copyOf(categorias);
    }

    /** Actividades por semana del último rango consultado. */
    public List<ConteoSemana> getSemanas() {
        return semanas;
    }

    public void establecerSemanas(List<ConteoSemana> semanas) {
        this.semanas = semanas == null ? List.of() : List.copyOf(semanas);
    }
}
