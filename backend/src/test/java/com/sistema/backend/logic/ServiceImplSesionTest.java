package com.sistema.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistema.common.excepciones.AutenticacionException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Administrador;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Usuario;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mindrot.jbcrypt.BCrypt;

@ExtendWith(MockitoExtension.class)
class ServiceImplSesionTest extends PruebaServicioBase {

    @Test
    @DisplayName("Inicia sesión con la clave correcta y devuelve el usuario sin la clave")
    void iniciaSesionConClaveCorrecta() {
        Funcionario ana = funcionario("222");
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(ana));
        when(usuarioDAO.buscarClaveHash("222")).thenReturn(Optional.of(HASH_CLAVE));

        Usuario autenticado = servicio.iniciarSesion("  222  ", "clave");

        assertEquals("222", autenticado.getId());
        assertEquals("Ana Pérez", ((Funcionario) autenticado).getNombre());
    }

    @Test
    @DisplayName("Inicia sesión como administrador")
    void iniciaSesionComoAdministrador() {
        Administrador admin = new Administrador("111");
        when(usuarioDAO.buscarPorId("111")).thenReturn(Optional.of(admin));
        when(usuarioDAO.buscarClaveHash("111")).thenReturn(Optional.of(HASH_CLAVE));

        assertTrue(servicio.iniciarSesion("111", "clave") instanceof Administrador);
    }

    @Test
    @DisplayName("Rechaza una clave incorrecta con el mismo mensaje que un usuario inexistente")
    void rechazaClaveIncorrecta() {
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(funcionario("222")));
        when(usuarioDAO.buscarClaveHash("222"))
                .thenReturn(Optional.of(BCrypt.hashpw("otra", BCrypt.gensalt(4))));

        AutenticacionException error = assertThrows(AutenticacionException.class,
                () -> servicio.iniciarSesion("222", "clave"));

        assertEquals("Identificador o clave incorrectos.", error.getMessage());
    }

    @Test
    @DisplayName("Rechaza un usuario inexistente")
    void rechazaUsuarioInexistente() {
        when(usuarioDAO.buscarPorId("999")).thenReturn(Optional.empty());
        when(usuarioDAO.buscarClaveHash("999")).thenReturn(Optional.empty());

        assertThrows(AutenticacionException.class,
                () -> servicio.iniciarSesion("999", "clave"));
    }

    @Test
    @DisplayName("Rechaza un hash malformado sin romper con una excepción inesperada")
    void rechazaHashMalformado() {
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(funcionario("222")));
        when(usuarioDAO.buscarClaveHash("222")).thenReturn(Optional.of("texto-plano"));

        assertThrows(AutenticacionException.class,
                () -> servicio.iniciarSesion("222", "clave"));
    }

    @Test
    @DisplayName("Valida identificador y clave vacíos antes de consultar la base")
    void validaCamposObligatorios() {
        assertThrows(ValidacionException.class, () -> servicio.iniciarSesion("  ", "clave"));
        assertThrows(ValidacionException.class, () -> servicio.iniciarSesion("222", " "));
        verifyNoInteractions(usuarioDAO);
    }

    @Test
    @DisplayName("Cambia la clave guardando un hash BCrypt nuevo")
    void cambiaClaveCorrectamente() {
        when(usuarioDAO.buscarClaveHash("222")).thenReturn(Optional.of(HASH_CLAVE));

        servicio.cambiarClave("222", "clave", "nueva-clave", "nueva-clave");

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(usuarioDAO).actualizarClave(org.mockito.ArgumentMatchers.eq("222"),
                hash.capture());
        assertTrue(BCrypt.checkpw("nueva-clave", hash.getValue()));
        assertFalse(hash.getValue().contains("nueva-clave"));
    }

    @Test
    @DisplayName("No cambia la clave si la actual es incorrecta")
    void rechazaClaveActualIncorrecta() {
        when(usuarioDAO.buscarClaveHash("222")).thenReturn(Optional.of(HASH_CLAVE));

        assertThrows(AutenticacionException.class,
                () -> servicio.cambiarClave("222", "incorrecta", "nueva", "nueva"));
        verify(usuarioDAO, never()).actualizarClave(anyString(), anyString());
    }

    @Test
    @DisplayName("No cambia la clave si la confirmación no coincide o es la misma")
    void validaConfirmacionYClaveDistinta() {
        assertThrows(ValidacionException.class,
                () -> servicio.cambiarClave("222", "clave", "nueva", "otra"));
        assertThrows(ValidacionException.class,
                () -> servicio.cambiarClave("222", "clave", "clave", "clave"));
        verifyNoInteractions(usuarioDAO);
    }

    @Test
    @DisplayName("No cambia la clave de un usuario inexistente")
    void rechazaUsuarioInexistenteAlCambiarClave() {
        when(usuarioDAO.buscarClaveHash("999")).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> servicio.cambiarClave("999", "clave", "nueva", "nueva"));
    }

    @Test
    @DisplayName("Rechaza una clave de más de 72 bytes")
    void rechazaClaveDemasiadoLarga() {
        String larga = "a".repeat(73);

        assertThrows(ValidacionException.class,
                () -> servicio.cambiarClave("222", "clave", larga, larga));
        assertThrows(ValidacionException.class,
                () -> servicio.iniciarSesion("222", larga));
        verifyNoInteractions(usuarioDAO);
    }

    @Test
    @DisplayName("Rechaza un identificador de más de 20 caracteres")
    void rechazaIdentificadorDemasiadoLargo() {
        assertThrows(ValidacionException.class,
                () -> servicio.iniciarSesion("1".repeat(21), "clave"));
        verifyNoInteractions(usuarioDAO);
    }
}
