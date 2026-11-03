package com.sistema.common.modelo;


public class Administrador extends Usuario {

    private static final long serialVersionUID = 1L;

    public Administrador(String id) {
        super(id, Rol.ADMINISTRADOR);
    }
}
