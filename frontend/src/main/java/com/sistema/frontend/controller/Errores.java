package com.sistema.frontend.controller;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Traduce un fallo a un mensaje que se pueda mostrar al usuario.
 *
 * <p>Las excepciones de dominio del backend ya traen un texto pensado para la
 * persona; los fallos inesperados se resumen con su tipo para no exponer
 * detalles internos.</p>
 */
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
        String tipo = causa.getClass().getSimpleName();
        return tipo.isBlank() ? MENSAJE_GENERICO : MENSAJE_GENERICO + " (" + tipo + ")";
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
