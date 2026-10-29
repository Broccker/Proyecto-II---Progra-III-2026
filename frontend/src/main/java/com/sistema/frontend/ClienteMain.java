package com.sistema.frontend;

import com.sistema.common.protocolo.Protocolo;
import com.sistema.frontend.config.ConfiguracionFrontend;
import com.sistema.frontend.controller.ControladorAplicacion;
import com.sistema.frontend.controller.EjecutorSwing;
import com.sistema.frontend.proxy.ExcepcionRed;
import com.sistema.frontend.proxy.ServiceProxy;
import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada del proceso Frontend (cliente Swing).
 *
 * <p>Abre la conexión con el backend, arranca el {@link ServiceProxy} y
 * entrega el control a {@link ControladorAplicacion}, que muestra el login y,
 * tras autenticar, la ventana principal.</p>
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

        ServiceProxy proxy;
        try {
            proxy = new ServiceProxy(configuracion);
        } catch (ExcepcionRed e) {
            reportar("Sin conexión con el backend", e.getMessage());
            return;
        }

        System.out.println("[frontend] Conectado al backend "
                + configuracion.host() + ":" + configuracion.puerto()
                + " (protocolo v" + Protocolo.VERSION + ").");

        ControladorAplicacion aplicacion =
                new ControladorAplicacion(proxy, new EjecutorSwing(), proxy, proxy);
        Runtime.getRuntime().addShutdownHook(new Thread(proxy::close, "cierre-proxy"));
        SwingUtilities.invokeLater(aplicacion::iniciar);
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
