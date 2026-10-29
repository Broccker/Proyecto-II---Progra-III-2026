package com.sistema.frontend.model;

import com.sistema.common.dto.MatrizActividades;

/**
 * Estado de la vista de actividades: matriz semanal cargada.
 */
public final class ModeloActividades {

    private MatrizActividades matriz;

    public MatrizActividades getMatriz() {
        return matriz;
    }

    public void establecerMatriz(MatrizActividades matriz) {
        this.matriz = matriz;
    }
}
