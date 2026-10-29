package com.sistema.common.modelo;

/**
 * Administrador del sistema: puede gestionar funcionarios, categorías,
 * recursos y consultar estadísticas.
 */
public class Administrador extends Usuario {

    private static final long serialVersionUID = 1L;

    public Administrador(String id) {
        super(id, Rol.ADMINISTRADOR);
    }
}
