package com.sistema.frontend.model;

import com.sistema.common.modelo.CategoriaRecurso;
import java.util.List;
import java.util.Objects;

/**
 * Estado del módulo de categorías: listado visible, criterio de búsqueda y
 * categoría seleccionada.
 */
public final class ModeloCategorias {

    private List<CategoriaRecurso> categorias = List.of();
    private CategoriaRecurso seleccionada;
    private String criterio = "";

    public List<CategoriaRecurso> getCategorias() {
        return categorias;
    }

    public void establecerCategorias(List<CategoriaRecurso> categorias) {
        Objects.requireNonNull(categorias, "La lista de categorías es obligatoria.");
        this.categorias = List.copyOf(categorias);
        if (seleccionada != null && !this.categorias.contains(seleccionada)) {
            seleccionada = null;
        }
    }

    public CategoriaRecurso getSeleccionada() {
        return seleccionada;
    }

    public void seleccionar(CategoriaRecurso categoria) {
        this.seleccionada = categoria;
    }

    public void limpiarSeleccion() {
        seleccionada = null;
    }

    public String getCriterio() {
        return criterio;
    }

    public void establecerCriterio(String criterio) {
        this.criterio = criterio == null ? "" : criterio.trim();
    }
}
