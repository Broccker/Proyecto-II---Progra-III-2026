package com.sistema.frontend.view;

import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Rol;
import com.sistema.common.modelo.Usuario;
import com.sistema.frontend.util.Iconos;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;


public final class VentanaPrincipal extends JFrame {

    private final JTabbedPane pestanas = new JTabbedPane();
    private final JLabel estadoSesion = new JLabel();
    private JComponent panelAlertas;

    private Runnable alCambiarClave;
    private Runnable alCerrarSesion;
    private Runnable alSalir;

    public VentanaPrincipal(Usuario usuario) {
        setTitle(usuario.getRol() == Rol.ADMINISTRADOR
                ? "Sistema de Reservas — Administrador"
                : "Sistema de Reservas — Funcionario");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(pestanas, BorderLayout.CENTER);
        add(crearBarraEstado(usuario), BorderLayout.SOUTH);
        setJMenuBar(crearMenu());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evento) {
                disparar(alSalir);
            }
        });
    }

    private JMenuBar crearMenu() {
        JMenu menuSesion = new JMenu("Sesión");

        JMenuItem cambiarClave = new JMenuItem("Cambiar clave");
        cambiarClave.addActionListener(e -> disparar(alCambiarClave));
        menuSesion.add(cambiarClave);

        JMenuItem cerrarSesion = new JMenuItem("Cerrar sesión");
        cerrarSesion.addActionListener(e -> disparar(alCerrarSesion));
        menuSesion.add(cerrarSesion);

        menuSesion.addSeparator();
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(e -> disparar(alSalir));
        menuSesion.add(salir);

        JMenuBar barra = new JMenuBar();
        barra.add(menuSesion);
        return barra;
    }

    private JLabel crearBarraEstado(Usuario usuario) {
        String nombre = usuario instanceof Funcionario funcionario
                ? funcionario.getNombre() : "Administrador";
        String rol = usuario.getRol() == Rol.ADMINISTRADOR ? "Administrador" : "Funcionario";
        estadoSesion.setText("  Sesión: " + nombre + " (" + usuario.getId() + ") — " + rol);
        estadoSesion.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return estadoSesion;
    }


    public void agregarPestana(String titulo, String icono, JComponent contenido) {
        pestanas.addTab(titulo, Iconos.pestana(icono), contenido);
    }


    public void agregarPanelAlertas(JComponent panel) {
        if (panelAlertas != null) {
            remove(panelAlertas);
        }
        panelAlertas = panel;
        if (panel != null) {
            add(panel, BorderLayout.EAST);
            revalidate();
            repaint();
        }
    }

    public JComponent getPanelAlertas() {
        return panelAlertas;
    }

    public void alCambiarClave(Runnable accion) {
        alCambiarClave = accion;
    }

    public void alCerrarSesion(Runnable accion) {
        alCerrarSesion = accion;
    }

    public void alSalir(Runnable accion) {
        alSalir = accion;
    }


    public void mostrar() {
        setVisible(true);
    }


    public void cerrar() {
        dispose();
    }

    public JTabbedPane getPestanas() {
        return pestanas;
    }

    private static void disparar(Runnable accion) {
        if (accion != null) {
            accion.run();
        }
    }
}
