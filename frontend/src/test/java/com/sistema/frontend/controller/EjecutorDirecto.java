package com.sistema.frontend.controller;

import java.util.function.Consumer;


final class EjecutorDirecto implements EjecutorTareas {

    private int ejecuciones;

    @Override
    public void ejecutar(Runnable trabajo, Runnable alTerminar, Consumer<Throwable> alFallar) {
        ejecuciones++;
        try {
            trabajo.run();
        } catch (Throwable error) {
            alFallar.accept(error);
            return;
        }
        alTerminar.run();
    }

    int ejecuciones() {
        return ejecuciones;
    }
}
