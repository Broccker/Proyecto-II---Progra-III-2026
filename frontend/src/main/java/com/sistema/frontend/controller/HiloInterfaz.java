package com.sistema.frontend.controller;

import javax.swing.SwingUtilities;


@FunctionalInterface
public interface HiloInterfaz {

    void ejecutar(Runnable tarea);


    static HiloInterfaz swing() {
        return SwingUtilities::invokeLater;
    }
}
