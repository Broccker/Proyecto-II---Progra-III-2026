package com.sistema.frontend.view;

import com.sistema.frontend.util.Iconos;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Formulario de cambio de clave.
 *
 * <p>Es un panel, no un diálogo: la ventana que lo contiene la construye el
 * orquestador de la aplicación. Así la vista se puede probar sin pantalla.</p>
 */
public final class VistaCambiarClave extends JPanel {

    private static final String TITULO = "Cambio de clave";

    private final JTextField campoId;
    private final JPasswordField campoClaveActual = new JPasswordField(15);
    private final JPasswordField campoNuevaClave = new JPasswordField(15);
    private final JPasswordField campoConfirmacion = new JPasswordField(15);
    private final JButton botonAceptar = new JButton("Aceptar", Iconos.boton("Aceptar"));
    private final JButton botonCancelar = new JButton("Cancelar", Iconos.boton("Cancelar"));

    private Runnable accionAceptar;
    private Runnable accionCancelar;

    public VistaCambiarClave(String idInicial, boolean idEditable) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        campoId = new JTextField(idInicial == null ? "" : idInicial, 15);
        campoId.setEditable(idEditable);

        add(crearFormulario(), BorderLayout.CENTER);
        add(crearBotones(), BorderLayout.SOUTH);

        botonAceptar.addActionListener(e -> disparar(accionAceptar));
        botonCancelar.addActionListener(e -> disparar(accionCancelar));
        campoConfirmacion.addActionListener(e -> disparar(accionAceptar));
    }

    private JPanel crearFormulario() {
        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(6, 8, 6, 8);
        restricciones.fill = GridBagConstraints.HORIZONTAL;

        agregarCampo(formulario, restricciones, 0, "Identificador:", campoId);
        agregarCampo(formulario, restricciones, 1, "Clave actual:", campoClaveActual);
        agregarCampo(formulario, restricciones, 2, "Clave nueva:", campoNuevaClave);
        agregarCampo(formulario, restricciones, 3, "Confirmación:", campoConfirmacion);
        return formulario;
    }

    private static void agregarCampo(JPanel panel, GridBagConstraints restricciones,
                                     int fila, String etiqueta, JTextField campo) {
        restricciones.gridx = 0;
        restricciones.gridy = fila;
        panel.add(new JLabel(etiqueta), restricciones);
        restricciones.gridx = 1;
        panel.add(campo, restricciones);
    }

    private JPanel crearBotones() {
        JPanel botones = new JPanel();
        botones.add(botonAceptar);
        botones.add(botonCancelar);
        return botones;
    }

    private static void disparar(Runnable accion) {
        if (accion != null) {
            accion.run();
        }
    }

    public void alAceptar(Runnable accion) {
        accionAceptar = accion;
    }

    public void alCancelar(Runnable accion) {
        accionCancelar = accion;
    }

    public String getId() {
        return campoId.getText().trim();
    }

    public String getClaveActual() {
        return new String(campoClaveActual.getPassword());
    }

    public String getNuevaClave() {
        return new String(campoNuevaClave.getPassword());
    }

    public String getConfirmacion() {
        return new String(campoConfirmacion.getPassword());
    }

    public void establecerId(String id) {
        campoId.setText(id == null ? "" : id);
    }

    public void establecerClaves(String actual, String nueva, String confirmacion) {
        campoClaveActual.setText(actual == null ? "" : actual);
        campoNuevaClave.setText(nueva == null ? "" : nueva);
        campoConfirmacion.setText(confirmacion == null ? "" : confirmacion);
    }

    public void limpiarClaves() {
        campoClaveActual.setText("");
        campoNuevaClave.setText("");
        campoConfirmacion.setText("");
    }

    public void habilitar(boolean habilitado) {
        campoId.setEnabled(habilitado);
        campoClaveActual.setEnabled(habilitado);
        campoNuevaClave.setEnabled(habilitado);
        campoConfirmacion.setEnabled(habilitado);
        botonAceptar.setEnabled(habilitado);
        botonCancelar.setEnabled(habilitado);
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
}
