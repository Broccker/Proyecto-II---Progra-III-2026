package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistema.common.excepciones.AutenticacionException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Usuario;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.Sesion;
import com.sistema.frontend.view.VistaLogin;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ControladorLoginTest {

    private Service servicio;
    private VistaLogin vista;
    private Sesion sesion;
    private EjecutorDirecto ejecutor;
    private ControladorLogin controlador;

    @BeforeEach
    void preparar() {
        servicio = mock(Service.class);
        vista = new VistaLogin();
        sesion = new Sesion();
        ejecutor = new EjecutorDirecto();
        controlador = new ControladorLogin(servicio, vista, sesion, ejecutor);
    }

    @Test
    @DisplayName("Con credenciales válidas registra la sesión y avisa al orquestador")
    void autenticaYRegistraLaSesion() {
        Funcionario ana = new Funcionario("222", "Ana Pérez", "88881111");
        when(servicio.iniciarSesion("222", "secreta")).thenReturn(ana);
        AtomicReference<Usuario> autenticado = new AtomicReference<>();
        controlador.alAutenticar(autenticado::set);

        vista.establecerId("222");
        vista.establecerClave("secreta");
        vista.getBotonIngresar().doClick();

        assertTrue(sesion.haySesion());
        assertSame(ana, sesion.getUsuario());
        assertSame(ana, autenticado.get());
        verify(servicio).iniciarSesion("222", "secreta");
    }

    @Test
    @DisplayName("Sin identificador no consulta al backend")
    void exigeElIdentificador() {
        vista.establecerClave("secreta");

        vista.getBotonIngresar().doClick();

        verifyNoInteractions(servicio);
        assertFalse(sesion.haySesion());
    }

    @Test
    @DisplayName("Sin clave no consulta al backend")
    void exigeLaClave() {
        vista.establecerId("222");

        vista.getBotonIngresar().doClick();

        verifyNoInteractions(servicio);
        assertFalse(sesion.haySesion());
    }

    @Test
    @DisplayName("Si el backend rechaza las credenciales no hay sesión")
    void credencialesIncorrectasNoAbrenSesion() {
        when(servicio.iniciarSesion("222", "mala"))
                .thenThrow(new AutenticacionException("Credenciales incorrectas."));
        vista.establecerId("222");
        vista.establecerClave("mala");

        vista.getBotonIngresar().doClick();

        assertFalse(sesion.haySesion());
        assertEquals(1, ejecutor.ejecuciones());
    }

    @Test
    @DisplayName("El botón de cambio de clave entrega el identificador escrito")
    void solicitaElCambioDeClaveConElIdentificador() {
        AtomicReference<String> idSolicitado = new AtomicReference<>();
        controlador.alSolicitarCambioClave(idSolicitado::set);
        vista.establecerId("333");

        controlador.solicitarCambioClave();

        assertEquals("333", idSolicitado.get());
    }

    @Test
    @DisplayName("Un usuario inesperadamente nulo no se registra como sesión")
    void usuarioNuloNoAbreSesion() {
        when(servicio.iniciarSesion("222", "clave"))
                .thenThrow(new ValidacionException("Respuesta inválida."));
        vista.establecerId("222");
        vista.establecerClave("clave");

        vista.getBotonIngresar().doClick();

        assertFalse(sesion.haySesion());
    }
}
