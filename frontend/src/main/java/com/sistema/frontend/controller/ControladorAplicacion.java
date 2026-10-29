package com.sistema.frontend.controller;

import com.sistema.common.modelo.Usuario;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.Sesion;
import com.sistema.frontend.proxy.FuenteNotificaciones;
import com.sistema.frontend.view.VistaCambiarClave;
import com.sistema.frontend.view.VistaLogin;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 * Orquesta el ciclo de vida de la aplicación de escritorio: login, ventana
 * principal, cambio de clave, cierre de sesión y salida.
 *
 * <p>Es el único lugar que crea ventanas. Los controladores de módulo reciben
 * vistas ya construidas y permanecen ajenos a los marcos.</p>
 */
public final class ControladorAplicacion {

    private final Service servicio;
    private final EjecutorTareas ejecutor;
    private final AutoCloseable conexion;
    private final FuenteNotificaciones notificaciones;
    private final Sesion sesion = new Sesion();

    private JFrame marcoLogin;
    private ControladorPrincipal principal;

    public ControladorAplicacion(Service servicio, EjecutorTareas ejecutor,
                                 AutoCloseable conexion,
                                 FuenteNotificaciones notificaciones) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio es obligatorio.");
        this.ejecutor = Objects.requireNonNull(ejecutor, "El ejecutor es obligatorio.");
        this.conexion = Objects.requireNonNull(conexion, "La conexión es obligatoria.");
        this.notificaciones = Objects.requireNonNull(notificaciones,
                "La fuente de notificaciones es obligatoria.");
    }

    /** Punto de entrada de la interfaz; debe invocarse en el hilo de eventos. */
    public void iniciar() {
        mostrarLogin();
    }

    private void mostrarLogin() {
        VistaLogin vista = new VistaLogin();
        ControladorLogin controlador = new ControladorLogin(servicio, vista, sesion, ejecutor);
        controlador.alAutenticar(this::abrirPrincipal);
        controlador.alSolicitarCambioClave(id -> abrirCambioClave(id, true));
        controlador.alSalir(this::salir);

        marcoLogin = new JFrame("Sistema de Reservas — Ingreso");
        marcoLogin.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        marcoLogin.add(vista);
        marcoLogin.setSize(vista.tamanoPreferido());
        marcoLogin.setLocationRelativeTo(null);
        marcoLogin.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evento) {
                salir();
            }
        });
        marcoLogin.setVisible(true);
    }

    private void abrirPrincipal(Usuario usuario) {
        cerrarLogin();
        principal = new ControladorPrincipal(sesion, servicio, ejecutor);
        principal.alCambiarClave(() -> abrirCambioClave(usuario.getId(), false));
        principal.alCerrarSesion(this::cerrarSesion);
        principal.alSalir(this::salir);
        notificaciones.agregarEscucha(principal.getControladorAlertas());
        principal.mostrar();
    }

    /**
     * Abre el diálogo modal de cambio de clave.
     *
     * @param id         identificador inicial
     * @param editable   si el usuario puede escribir otro identificador
     */
    public void abrirCambioClave(String id, boolean editable) {
        VistaCambiarClave vista = new VistaCambiarClave(id, editable);
        ControladorCambiarClave controlador =
                new ControladorCambiarClave(servicio, vista, ejecutor);
        JDialog dialogo = new JDialog(ventanaActiva(), "Cambio de clave", true);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.add(vista);
        dialogo.pack();
        dialogo.setLocationRelativeTo(ventanaActiva());
        controlador.alTerminar(dialogo::dispose);
        controlador.alCancelar(dialogo::dispose);
        dialogo.setVisible(true);
    }

    private Frame ventanaActiva() {
        if (principal != null) {
            return principal.getVentana();
        }
        return marcoLogin;
    }

    private void cerrarSesion() {
        descartarAlertas();
        if (principal != null) {
            principal.cerrar();
            principal = null;
        }
        sesion.cerrar();
        mostrarLogin();
    }

    private void cerrarLogin() {
        if (marcoLogin != null) {
            marcoLogin.dispose();
            marcoLogin = null;
        }
    }

    /** Cierra ventanas y conexión. Es idempotente. */
    public void cerrarTodo() {
        descartarAlertas();
        if (principal != null) {
            principal.cerrar();
            principal = null;
        }
        cerrarLogin();
        sesion.cerrar();
        try {
            conexion.close();
        } catch (Exception e) {
            System.err.println("[frontend] No se pudo cerrar la conexión: " + e.getMessage());
        }
    }

    /**
     * Deja de escuchar notificaciones y borra las alertas de la sesión. Las
     * alertas no se persisten: al cerrar sesión se pierden.
     */
    private void descartarAlertas() {
        ControladorPrincipal actual = principal;
        if (actual == null) {
            return;
        }
        notificaciones.quitarEscucha(actual.getControladorAlertas());
        actual.getControladorAlertas().vaciar();
    }

    private void salir() {
        cerrarTodo();
        System.exit(0);
    }
}
