package com.sistema.frontend;

import com.sistema.common.protocolo.Protocolo;
import com.sistema.frontend.config.ConfiguracionFrontend;
import com.sistema.frontend.controller.ControladorAplicacion;
import com.sistema.frontend.controller.EjecutorSwing;
import com.sistema.frontend.proxy.ExcepcionRed;
import com.sistema.frontend.proxy.ServiceProxy;
import com.sistema.frontend.util.Localizacion;
import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


public final class ClienteMain {

    private ClienteMain() {
    }

    public static void main(String[] args) {
        instalarLookAndFeel();
        Localizacion.aplicar();

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
