package com.sistema.frontend;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada del proceso Frontend (cliente Swing).
 *
 * <p>En la Fase 7 este main se conecta al backend mediante el proxy; en la
 * Fase 8 abre la ventana de login.</p>
 */
public final class ClienteMain {

    private ClienteMain() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Se mantiene el look and feel por defecto.
            }
            System.out.println("[frontend] Sistema de Reservas de Recursos - Proyecto 2");
            System.out.println("[frontend] Java " + System.getProperty("java.version")
                    + " | conexion con el backend pendiente (Fase 7).");
        });
    }
}
