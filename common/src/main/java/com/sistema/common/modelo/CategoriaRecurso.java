package com.sistema.common.modelo;

import com.sistema.common.excepciones.ValidacionException;
import java.io.Serializable;
import java.util.Objects;

/**
 * Categoría a la que pertenece un recurso (proyectores, salas, etc.).
 */
public class CategoriaRecurso implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String descripcion;

    public CategoriaRecurso(String id, String descripcion) {
        setId(id);
        setDescripcion(descripcion);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new ValidacionException("El identificador de la categoría no puede estar vacío.");
        }
        this.id = id.trim();
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new ValidacionException("La descripción de la categoría no puede estar vacía.");
        }
        this.descripcion = descripcion.trim();
    }

    @Override
    public String toString() {
        return descripcion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CategoriaRecurso otra)) {
            return false;
        }
        return Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
