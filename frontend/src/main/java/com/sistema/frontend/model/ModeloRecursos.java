package com.sistema.frontend.model;

import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Recurso;
import java.util.List;
import java.util.Objects;

/**
 * Estado del módulo de recursos: catálogo de categorías para los filtros, lista
 * visible, criterios de búsqueda y recurso seleccionado.
 */
public final class ModeloRecursos {

    private List<Recurso> recursos = List.of();
    private List<CategoriaRecurso> categorias = List.of();
    private Recurso seleccionado;
    private String categoriaFiltroId;
    private String criterio = "";

    public List<Recurso> getRecursos() {
        return recursos;
    }

    public void establecerRecursos(List<Recurso> recursos) {
        Objects.requireNonNull(recursos, "La lista de recursos es obligatoria.");
        this.recursos = List.copyOf(recursos);
        if (seleccionado != null && !this.recursos.contains(seleccionado)) {
            seleccionado = null;
        }
    }

    public List<CategoriaRecurso> getCategorias() {
        return categorias;
    }

    public void establecerCategorias(List<CategoriaRecurso> categorias) {
        Objects.requireNonNull(categorias, "La lista de categorías es obligatoria.");
        this.categorias = List.copyOf(categorias);
    }

    public CategoriaRecurso buscarCategoria(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return categorias.stream()
                .filter(categoria -> categoria.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public Recurso getSeleccionado() {
        return seleccionado;
    }

    public void seleccionar(Recurso recurso) {
        this.seleccionado = recurso;
    }

    public void limpiarSeleccion() {
        seleccionado = null;
    }

    public String getCategoriaFiltroId() {
        return categoriaFiltroId;
    }

    public void establecerCategoriaFiltro(String categoriaId) {
        categoriaFiltroId = categoriaId == null || categoriaId.isBlank() ? null : categoriaId;
    }

    public String getCriterio() {
        return criterio;
    }

    public void establecerCriterio(String criterio) {
        this.criterio = criterio == null ? "" : criterio.trim();
    }
}
