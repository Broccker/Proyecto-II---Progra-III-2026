package com.sistema.frontend.controller;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;


public final class Errores {

    private static final String MENSAJE_GENERICO = "Ocurrió un error inesperado.";

    private Errores() {
    }

    public static String mensaje(Throwable error) {
        Throwable causa = desenrollar(error);
        if (causa == null) {
            return MENSAJE_GENERICO;
        }
        String texto = causa.getMessage();
        if (texto != null && !texto.isBlank()) {
            return texto;
        }
        return MENSAJE_GENERICO;
    }

    private static Throwable desenrollar(Throwable error) {
        Throwable actual = error;
        while ((actual instanceof ExecutionException || actual instanceof CompletionException)
                && actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual;
    }
}
