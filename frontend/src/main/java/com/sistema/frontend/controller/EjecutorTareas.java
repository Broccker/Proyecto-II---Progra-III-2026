package com.sistema.frontend.controller;

import java.util.function.Consumer;


@FunctionalInterface
public interface EjecutorTareas {


    void ejecutar(Runnable trabajo, Runnable alTerminar, Consumer<Throwable> alFallar);
}
