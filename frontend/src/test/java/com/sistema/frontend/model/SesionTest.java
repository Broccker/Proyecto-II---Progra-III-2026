package com.sistema.frontend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Administrador;
import com.sistema.common.modelo.Funcionario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SesionTest {

    @Test
    @DisplayName("La sesión recuerda al usuario y su rol")
    void registraElUsuario() {
        Sesion sesion = new Sesion();
        Funcionario ana = new Funcionario("222", "Ana Pérez", "88881111");

        sesion.iniciar(ana);

        assertTrue(sesion.haySesion());
        assertSame(ana, sesion.getUsuario());
        assertSame(ana, sesion.comoFuncionario());
        assertFalse(sesion.esAdministrador());
    }

    @Test
    @DisplayName("Cerrar la sesión olvida al usuario")
    void cierraLaSesion() {
        Sesion sesion = new Sesion();
        sesion.iniciar(new Administrador("111"));

        sesion.cerrar();

        assertFalse(sesion.haySesion());
        assertFalse(sesion.esAdministrador());
        assertThrows(ValidacionException.class, sesion::comoFuncionario);
    }

    @Test
    @DisplayName("Un administrador no se puede usar como funcionario")
    void administradorNoEsFuncionario() {
        Sesion sesion = new Sesion();
        sesion.iniciar(new Administrador("111"));

        assertTrue(sesion.esAdministrador());
        ValidacionException error = assertThrows(ValidacionException.class,
                sesion::comoFuncionario);
        assertEquals("La sesión activa no corresponde a un funcionario.", error.getMessage());
    }

    @Test
    @DisplayName("No se puede iniciar sesión con un usuario nulo")
    void usuarioNuloNoEsValido() {
        Sesion sesion = new Sesion();

        assertThrows(ValidacionException.class, () -> sesion.iniciar(null));
    }
}
