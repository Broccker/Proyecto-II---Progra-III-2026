package com.sistema.frontend.model;

import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Rol;
import com.sistema.common.modelo.Usuario;


public final class Sesion {

    private Usuario usuario;


    public void iniciar(Usuario usuario) {
        if (usuario == null) {
            throw new ValidacionException("El usuario autenticado es obligatorio.");
        }
        this.usuario = usuario;
    }


    public void cerrar() {
        usuario = null;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public boolean haySesion() {
        return usuario != null;
    }

    public boolean esAdministrador() {
        return usuario != null && usuario.getRol() == Rol.ADMINISTRADOR;
    }


    public Funcionario comoFuncionario() {
        if (usuario instanceof Funcionario funcionario) {
            return funcionario;
        }
        throw new ValidacionException("La sesión activa no corresponde a un funcionario.");
    }
}
