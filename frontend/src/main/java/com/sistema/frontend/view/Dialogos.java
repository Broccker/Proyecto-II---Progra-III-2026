package com.sistema.frontend.view;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;

/**
 * Mensajes al usuario con un único punto de salida.
 *
 * <p>En un entorno sin pantalla (pruebas automatizadas) no se abre ningún
 * diálogo: el mensaje se escribe en la salida estándar y
 * {@link #confirmar(Component, String, String)} responde que sí. Así los
 * controladores se pueden probar sin intervención manual.</p>
 */
public final class Dialogos {

    private Dialogos() {
    }

    /** Muestra un mensaje informativo. */
    public static void informar(Component padre, String titulo, String mensaje) {
        mostrar(padre, titulo, mensaje, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Muestra una advertencia. */
    public static void advertir(Component padre, String titulo, String mensaje) {
        mostrar(padre, titulo, mensaje, JOptionPane.WARNING_MESSAGE);
    }

    /** Muestra un error. */
    public static void error(Component padre, String titulo, String mensaje) {
        mostrar(padre, titulo, mensaje, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Pide confirmación al usuario. Sin pantalla devuelve {@code true} para que
     * las pruebas puedan recorrer el flujo completo.
     */
    public static boolean confirmar(Component padre, String titulo, String mensaje) {
        if (GraphicsEnvironment.isHeadless()) {
            return true;
        }
        return JOptionPane.showConfirmDialog(padre, mensaje, titulo,
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private static void mostrar(Component padre, String titulo, String mensaje, int tipo) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[" + titulo + "] " + mensaje);
            return;
        }
        JOptionPane.showMessageDialog(padre, mensaje, titulo, tipo);
    }
}
