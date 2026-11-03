package com.sistema.frontend.view;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;

public final class Dialogos {

    private static final Object[] OPCIONES_SI_NO = {"Sí", "No"};
    private static final Object[] OPCION_ACEPTAR = {"Aceptar"};

    private Dialogos() {
    }

    public static void informar(Component padre, String titulo, String mensaje) {
        mostrar(padre, titulo, mensaje, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void advertir(Component padre, String titulo, String mensaje) {
        mostrar(padre, titulo, mensaje, JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component padre, String titulo, String mensaje) {
        mostrar(padre, titulo, mensaje, JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirmar(Component padre, String titulo, String mensaje) {
        if (GraphicsEnvironment.isHeadless()) {
            return true;
        }
        int seleccion = JOptionPane.showOptionDialog(padre, mensaje, titulo,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                OPCIONES_SI_NO, OPCIONES_SI_NO[0]);
        return seleccion == 0;
    }

    private static void mostrar(Component padre, String titulo, String mensaje, int tipo) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[" + titulo + "] " + mensaje);
            return;
        }
        JOptionPane.showOptionDialog(padre, mensaje, titulo, JOptionPane.DEFAULT_OPTION,
                tipo, null, OPCION_ACEPTAR, OPCION_ACEPTAR[0]);
    }
}
