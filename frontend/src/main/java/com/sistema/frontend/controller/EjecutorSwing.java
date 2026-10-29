package com.sistema.frontend.controller;

import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/**
 * Implementación de {@link EjecutorTareas} basada en {@link SwingWorker}.
 *
 * <p>{@code doInBackground} corre en un hilo de trabajo y {@code done} en el
 * hilo de eventos, de modo que la interfaz nunca se congela mientras el proxy
 * espera la respuesta del backend.</p>
 */
public final class EjecutorSwing implements EjecutorTareas {

    @Override
    public void ejecutar(Runnable trabajo, Runnable alTerminar, Consumer<Throwable> alFallar) {
        Objects.requireNonNull(trabajo, "El trabajo es obligatorio.");
        Objects.requireNonNull(alTerminar, "El callback de éxito es obligatorio.");
        Objects.requireNonNull(alFallar, "El callback de error es obligatorio.");
        new SwingWorker<Void, Void>() {

            private Throwable error;

            @Override
            protected Void doInBackground() {
                try {
                    trabajo.run();
                } catch (Throwable fallo) {
                    error = fallo;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    alFallar.accept(error);
                } else {
                    alTerminar.run();
                }
            }
        }.execute();
    }
}
