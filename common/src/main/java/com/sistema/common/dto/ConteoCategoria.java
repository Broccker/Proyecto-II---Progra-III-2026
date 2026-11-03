package com.sistema.common.dto;

import com.sistema.common.modelo.CategoriaRecurso;
import java.io.Serializable;


public record ConteoCategoria(CategoriaRecurso categoria, long cantidad) implements Serializable {

    public ConteoCategoria {
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría del conteo no puede ser nula.");
        }
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        }
    }
}
