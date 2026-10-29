package com.sistema.common.modelo;

import com.sistema.common.excepciones.ValidacionException;
import java.util.regex.Pattern;

/**
 * Funcionario que puede registrar reservas de recursos.
 */
public class Funcionario extends Usuario {

    private static final long serialVersionUID = 1L;
    private static final Pattern PATRON_TELEFONO = Pattern.compile("^\\+?[0-9]{7,15}$");

    private String nombre;
    private String telefono;

    public Funcionario(String id, String nombre, String telefono) {
        super(id, Rol.FUNCIONARIO);
        setNombre(nombre);
        setTelefono(telefono);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidacionException("El nombre del funcionario no puede estar vacío.");
        }
        this.nombre = nombre.trim();
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        if (telefono == null || !PATRON_TELEFONO.matcher(telefono.trim()).matches()) {
            throw new ValidacionException(
                    "El teléfono no es válido. Use solo dígitos (7 a 15), con un + opcional al inicio.");
        }
        this.telefono = telefono.trim();
    }
}
