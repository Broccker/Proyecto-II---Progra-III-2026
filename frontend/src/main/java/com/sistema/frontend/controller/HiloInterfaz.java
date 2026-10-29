package com.sistema.frontend.controller;

import javax.swing.SwingUtilities;

/**
 * Entrega tareas al hilo de la interfaz.
 *
 * <p>Las notificaciones llegan por el hilo lector del proxy; para tocar
 * componentes Swing hay que volver al hilo de eventos. En producción se usa
 * {@link #swing()}; en las pruebas se inyecta una ejecución directa.</p>
 */
@FunctionalInterface
public interface HiloInterfaz {

    void ejecutar(Runnable tarea);

    /** Implementación real basada en {@link SwingUtilities#invokeLater}. */
    static HiloInterfaz swing() {
        return SwingUtilities::invokeLater;
    }
}
