package com.sistema.frontend.controller;

import java.util.function.Consumer;

/**
 * Ejecuta trabajo de red fuera del hilo de eventos de Swing.
 *
 * <p>Los controladores nunca llaman al {@code Service} directamente desde un
 * manejador de la vista: entregan el trabajo a esta abstracción, que en
 * producción usa {@link EjecutorSwing} y en las pruebas puede ejecutarse de
 * forma síncrona.</p>
 *
 * <p>El contrato es: primero se ejecuta {@code trabajo}; si termina sin error
 * se invoca {@code alTerminar} y, si lanza, se invoca {@code alFallar}. Ambos
 * callbacks se ejecutan en el hilo de eventos de Swing.</p>
 */
@FunctionalInterface
public interface EjecutorTareas {

    /**
     * @param trabajo    operación potencialmente lenta (llamada al backend)
     * @param alTerminar se ejecuta en el hilo de la interfaz si todo salió bien
     * @param alFallar   recibe el error si el trabajo no pudo completarse
     */
    void ejecutar(Runnable trabajo, Runnable alTerminar, Consumer<Throwable> alFallar);
}
