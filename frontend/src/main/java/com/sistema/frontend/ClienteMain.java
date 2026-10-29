package com.sistema.frontend;

import com.sistema.common.protocolo.Protocolo;
import com.sistema.frontend.config.ConfiguracionFrontend;
import com.sistema.frontend.proxy.ExcepcionRed;
import com.sistema.frontend.proxy.ServiceProxy;
import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada del proceso Frontend (cliente Swing).
 *
 * <p>En la Fase 7 comprueba la conexión con el backend y deja el
 * {@link ServiceProxy} listo para inyectarse. En la Fase 8 abrirá la ventana
 * de login usando ese mismo proxy como {@code Service}.</p>
 *
 * <p>Uso: {@code ClienteMain [host] [puerto]}. Sin argumentos toma el destino
 * de {@code frontend.properties} (o de los valores por defecto).</p>
 */
public final class ClienteMain {

    private ClienteMain() {
    }

    public static void main(String[] args) {
        instalarLookAndFeel();

        ConfiguracionFrontend configuracion;
        try {
            configuracion = ConfiguracionFrontend.cargar().conArgumentos(args);
        } catch (RuntimeException e) {
            reportar("Configuración inválida", e.getMessage());
            return;
        }

        try (ServiceProxy proxy = new ServiceProxy(configuracion)) {
            System.out.println("[frontend] Conectado al backend "
                    + configuracion.host() + ":" + configuracion.puerto()
                    + " (protocolo v" + Protocolo.VERSION + ").");
            System.out.println("[frontend] La ventana de login se habilita en la Fase 8.");
            // Fase 8: abrir aquí la ventana de login con `proxy` inyectado como Service.
        } catch (ExcepcionRed e) {
            reportar("Sin conexión con el backend", e.getMessage());
        }
    }

    private static void instalarLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            // Se mantiene el look and feel por defecto.
        }
    }

    private static void reportar(String titulo, String mensaje) {
        System.err.println("[frontend] " + titulo + ": " + mensaje);
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, mensaje,
                titulo, JOptionPane.ERROR_MESSAGE));
    }
}
