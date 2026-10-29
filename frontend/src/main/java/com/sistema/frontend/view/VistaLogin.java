package com.sistema.frontend.view;

import com.sistema.frontend.util.Iconos;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Vista de ingreso al sistema.
 *
 * <p>No conoce el proxy ni las reglas de negocio: solo recoge las credenciales
 * y expone acciones que el controlador conecta.</p>
 */
public final class VistaLogin extends JPanel {

    private static final String TITULO = "Ingreso";

    private final JTextField campoId = new JTextField(15);
    private final JPasswordField campoClave = new JPasswordField(15);
    private final JButton botonIngresar = new JButton("Ingresar", Iconos.boton("Ingresar"));
    private final JButton botonCambiarClave =
            new JButton("Cambiar clave", Iconos.boton("CambiarContra"));
    private final JButton botonSalir = new JButton("Salir", Iconos.boton("Cancelar"));

    private Runnable accionIngresar;
    private Runnable accionCambiarClave;
    private Runnable accionSalir;

    public VistaLogin() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearFormulario(), BorderLayout.CENTER);
        add(crearBotones(), BorderLayout.SOUTH);

        botonIngresar.addActionListener(e -> disparar(accionIngresar));
        botonCambiarClave.addActionListener(e -> disparar(accionCambiarClave));
        botonSalir.addActionListener(e -> disparar(accionSalir));
        campoClave.addActionListener(e -> disparar(accionIngresar));
    }

    private JPanel crearEncabezado() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel titulo = new JLabel("Sistema de Reservas de Recursos",
                SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitulo = new JLabel("Proyecto 2 — ingrese sus credenciales",
                SwingConstants.CENTER);
        panel.add(titulo, BorderLayout.NORTH);
        panel.add(subtitulo, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        return panel;
    }

    private JPanel crearFormulario() {
        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(6, 8, 6, 8);
        restricciones.fill = GridBagConstraints.HORIZONTAL;

        restricciones.gridx = 0;
        restricciones.gridy = 0;
        formulario.add(new JLabel("Identificador:", SwingConstants.RIGHT), restricciones);

        restricciones.gridx = 1;
        campoId.setToolTipText("Identificador del funcionario o del administrador");
        formulario.add(campoId, restricciones);

        restricciones.gridx = 0;
        restricciones.gridy = 1;
        formulario.add(new JLabel("Clave:", SwingConstants.RIGHT), restricciones);

        restricciones.gridx = 1;
        formulario.add(campoClave, restricciones);
        return formulario;
    }

    private JPanel crearBotones() {
        JPanel botones = new JPanel();
        botones.add(botonIngresar);
        botones.add(botonCambiarClave);
        botones.add(botonSalir);
        return botones;
    }

    private static void disparar(Runnable accion) {
        if (accion != null) {
            accion.run();
        }
    }

    /** Asocia la acción del botón Ingresar (y de Intro en la clave). */
    public void alIngresar(Runnable accion) {
        accionIngresar = accion;
    }

    /** Asocia la acción del botón Cambiar clave. */
    public void alCambiarClave(Runnable accion) {
        accionCambiarClave = accion;
    }

    /** Asocia la acción del botón Salir. */
    public void alSalir(Runnable accion) {
        accionSalir = accion;
    }

    public String getId() {
        return campoId.getText().trim();
    }

    public String getClave() {
        return new String(campoClave.getPassword());
    }

    public void establecerId(String id) {
        campoId.setText(id == null ? "" : id);
    }

    public void establecerClave(String clave) {
        campoClave.setText(clave == null ? "" : clave);
    }

    public void limpiarClave() {
        campoClave.setText("");
        campoClave.requestFocusInWindow();
    }

    public void limpiar() {
        campoId.setText("");
        campoClave.setText("");
        campoId.requestFocusInWindow();
    }

    /** Habilita o deshabilita el formulario mientras se espera al backend. */
    public void habilitar(boolean habilitado) {
        campoId.setEnabled(habilitado);
        campoClave.setEnabled(habilitado);
        botonIngresar.setEnabled(habilitado);
        botonCambiarClave.setEnabled(habilitado);
    }

    public void mostrarInformacion(String mensaje) {
        Dialogos.informar(this, TITULO, mensaje);
    }

    public void mostrarAdvertencia(String mensaje) {
        Dialogos.advertir(this, TITULO, mensaje);
    }

    public void mostrarError(String mensaje) {
        Dialogos.error(this, TITULO, mensaje);
    }

    /** Tamaño preferido de la ventana de ingreso. */
    public Dimension tamanoPreferido() {
        return new Dimension(440, 300);
    }

    public JButton getBotonIngresar() {
        return botonIngresar;
    }
}
