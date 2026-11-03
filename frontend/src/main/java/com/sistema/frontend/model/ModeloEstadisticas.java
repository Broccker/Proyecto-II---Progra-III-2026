package com.sistema.frontend.model;

import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import java.util.List;


public final class ModeloEstadisticas {

    private List<ConteoCategoria> categorias = List.of();
    private List<ConteoSemana> semanas = List.of();


    public List<ConteoCategoria> getCategorias() {
        return categorias;
    }

    public void establecerCategorias(List<ConteoCategoria> categorias) {
        this.categorias = categorias == null ? List.of() : List.copyOf(categorias);
    }


    public List<ConteoSemana> getSemanas() {
        return semanas;
    }

    public void establecerSemanas(List<ConteoSemana> semanas) {
        this.semanas = semanas == null ? List.of() : List.copyOf(semanas);
    }
}
