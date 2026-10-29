package com.sistema.frontend.model;

import com.sistema.common.dto.MatrizCalendarizacion;
import com.sistema.common.modelo.CategoriaRecurso;
import java.util.List;
import java.util.Objects;

/**
 * Estado de la calendarización: categorías disponibles, categoría consultada y
 * matriz cargada.
 */
public final class ModeloCalendarizacion {

    private List<CategoriaRecurso> categorias = List.of();
    private MatrizCalendarizacion matriz;
    private String categoriaId;

    public List<CategoriaRecurso> getCategorias() {
        return categorias;
    }

    public void establecerCategorias(List<CategoriaRecurso> categorias) {
        Objects.requireNonNull(categorias, "La lista de categorías es obligatoria.");
        this.categorias = List.copyOf(categorias);
    }

    public MatrizCalendarizacion getMatriz() {
        return matriz;
    }

    public void establecerMatriz(MatrizCalendarizacion matriz) {
        this.matriz = matriz;
        if (matriz != null) {
            categoriaId = matriz.categoria().getId();
        }
    }

    public String getCategoriaId() {
        return categoriaId;
    }

    public void establecerCategoria(String categoriaId) {
        this.categoriaId = categoriaId == null || categoriaId.isBlank() ? null : categoriaId;
    }
}
