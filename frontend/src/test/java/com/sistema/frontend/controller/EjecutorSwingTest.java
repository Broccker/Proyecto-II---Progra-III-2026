package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class EjecutorSwingTest {

    @Test
    @Timeout(15)
    @DisplayName("El trabajo corre fuera del hilo de eventos y el éxito vuelve a él")
    void ejecutaFueraDelHiloDeEventos() throws Exception {
        EjecutorSwing ejecutor = new EjecutorSwing();
        CountDownLatch terminado = new CountDownLatch(1);
        AtomicBoolean enHiloDeEventos = new AtomicBoolean(true);
        AtomicBoolean exitoEnHiloDeEventos = new AtomicBoolean(false);

        SwingUtilities.invokeAndWait(() -> ejecutor.ejecutar(
                () -> enHiloDeEventos.set(SwingUtilities.isEventDispatchThread()),
                () -> {
                    exitoEnHiloDeEventos.set(SwingUtilities.isEventDispatchThread());
                    terminado.countDown();
                },
                error -> terminado.countDown()));

        assertTrue(terminado.await(10, TimeUnit.SECONDS));
        assertFalse(enHiloDeEventos.get(), "El trabajo debe correr fuera del EDT.");
        assertTrue(exitoEnHiloDeEventos.get(), "El callback debe correr en el EDT.");
    }

    @Test
    @Timeout(15)
    @DisplayName("Un fallo se entrega al callback de error")
    void entregaElFallo() throws Exception {
        EjecutorSwing ejecutor = new EjecutorSwing();
        CountDownLatch terminado = new CountDownLatch(1);
        AtomicReference<Throwable> capturado = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> ejecutor.ejecutar(
                () -> {
                    throw new IllegalStateException("fallo de prueba");
                },
                terminado::countDown,
                error -> {
                    capturado.set(error);
                    terminado.countDown();
                }));

        assertTrue(terminado.await(10, TimeUnit.SECONDS));
        assertEquals("fallo de prueba", capturado.get().getMessage());
    }
}
