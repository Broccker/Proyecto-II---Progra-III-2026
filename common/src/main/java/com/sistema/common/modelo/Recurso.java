package com.sistema.common.modelo;

import com.sistema.common.excepciones.ValidacionException;
import java.io.Serializable;
import java.util.Objects;

/**
 * Recurso físico que se puede reservar (proyector, sala, laptop, etc.).
 */
public class Recurso implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private CategoriaRecurso categoria;
    private String descripcion;

    public Recurso(String id, CategoriaRecurso categoria, String descripcion) {
        setId(id);
        setCategoria(categoria);
        setDescripcion(descripcion);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new ValidacionException("El identificador del recurso no puede estar vacío.");
        }
        this.id = id.trim();
    }

    public CategoriaRecurso getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaRecurso categoria) {
        if (categoria == null) {
            throw new ValidacionException("El recurso debe pertenecer a una categoría.");
        }
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new ValidacionException("La descripción del recurso no puede estar vacía.");
        }
        this.descripcion = descripcion.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Recurso otro)) {
            return false;
        }
        return Objects.equals(id, otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
