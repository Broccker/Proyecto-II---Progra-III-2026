package com.sistema.common.modelo;

import com.sistema.common.excepciones.ValidacionException;
import java.io.Serializable;
import java.util.Objects;

/**
 * Usuario del sistema. Puede ser administrador o funcionario.
 *
 * <p>El modelo <strong>no contiene la clave</strong>: el hash solo existe en el
 * backend, de modo que nunca se envía por la red ni se muestra en el frontend.</p>
 */
public abstract class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final Rol rol;

    protected Usuario(String id, Rol rol) {
        if (id == null || id.isBlank()) {
            throw new ValidacionException("El identificador del usuario no puede estar vacío.");
        }
        if (rol == null) {
            throw new ValidacionException("El rol del usuario no puede ser nulo.");
        }
        this.id = id.trim();
        this.rol = rol;
    }

    public String getId() {
        return id;
    }

    public Rol getRol() {
        return rol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario otro)) {
            return false;
        }
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
