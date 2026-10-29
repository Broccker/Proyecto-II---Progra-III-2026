package com.sistema.frontend.model;

import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Rol;
import com.sistema.common.modelo.Usuario;

/**
 * Estado de la sesión activa: el usuario autenticado.
 *
 * <p>La clave nunca forma parte del modelo: el backend no la devuelve.</p>
 */
public final class Sesion {

    private Usuario usuario;

    /** Registra al usuario que acaba de autenticarse. */
    public void iniciar(Usuario usuario) {
        if (usuario == null) {
            throw new ValidacionException("El usuario autenticado es obligatorio.");
        }
        this.usuario = usuario;
    }

    /** Cierra la sesión y olvida al usuario. */
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

    /**
     * Devuelve el usuario como funcionario.
     *
     * @throws ValidacionException si la sesión no es de un funcionario
     */
    public Funcionario comoFuncionario() {
        if (usuario instanceof Funcionario funcionario) {
            return funcionario;
        }
        throw new ValidacionException("La sesión activa no corresponde a un funcionario.");
    }
}
