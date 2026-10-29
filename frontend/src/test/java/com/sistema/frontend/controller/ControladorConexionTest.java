package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sistema.frontend.proxy.ExcepcionRed;
import com.sistema.frontend.proxy.Reconectable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ControladorConexionTest {

    private static final ExcepcionRed CORTE = new ExcepcionRed(
            ExcepcionRed.Motivo.CONEXION_PERDIDA, "Se perdió la conexión con el backend.");

    @Test
    @DisplayName("Al perder la conexión intenta reconectar y avisa el éxito")
    void reconectaYRestablece() {
        AtomicInteger intentos = new AtomicInteger();
        AtomicBoolean cerrada = new AtomicBoolean();
        AtomicBoolean restablecida = new AtomicBoolean();
        ControladorConexion controlador = new ControladorConexion(
                intentos::incrementAndGet, new EjecutorDirecto(), Runnable::run,
                () -> cerrada.set(true));
        controlador.alRestablecer(() -> restablecida.set(true));

        controlador.alPerderConexion(CORTE);

        assertEquals(1, intentos.get());
        assertTrue(restablecida.get());
        assertFalse(cerrada.get());
    }

    @Test
    @DisplayName("Si el backend sigue apagado cierra la aplicación tras los intentos")
    void cierraSiNoPuedeReconectar() {
        AtomicInteger intentos = new AtomicInteger();
        AtomicBoolean cerrada = new AtomicBoolean();
        Reconectable reconectable = () -> {
            intentos.incrementAndGet();
            throw new ExcepcionRed(ExcepcionRed.Motivo.SIN_CONEXION,
                    "No se pudo conectar con el backend.");
        };
        ControladorConexion controlador = new ControladorConexion(
                reconectable, new EjecutorDirecto(), Runnable::run,
                () -> cerrada.set(true));

        controlador.alPerderConexion(CORTE);

        assertEquals(3, intentos.get(), "Debe respetar el máximo de intentos.");
        assertTrue(cerrada.get(), "Sin reconexión debe cerrar de forma ordenada.");
    }

    @Test
    @DisplayName("Las notificaciones normales no las procesa este controlador")
    void ignoraNotificaciones() {
        Reconectable reconectable = mock(Reconectable.class);
        ControladorConexion controlador = new ControladorConexion(
                reconectable, new EjecutorDirecto(), Runnable::run, () -> { });

        controlador.alRecibir(null);

        verifyNoInteractions(reconectable);
    }
}
