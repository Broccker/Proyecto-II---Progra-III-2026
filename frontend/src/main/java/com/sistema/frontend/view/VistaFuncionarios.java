package com.sistema.frontend.view;

import com.sistema.common.modelo.Funcionario;
import com.sistema.frontend.model.ModeloFuncionarios;
import com.sistema.frontend.util.Iconos;
import com.sistema.frontend.view.componentes.ModeloTabla;
import com.sistema.frontend.view.componentes.SelectorArchivoPdf;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Vista del módulo de funcionarios.
 *
 * <p>Renderiza el {@link ModeloFuncionarios} y notifica las intenciones del
 * usuario mediante acciones; toda la coordinación queda en el controlador.</p>
 */
public final class VistaFuncionarios extends JPanel {

    private static final String TITULO = "Funcionarios";
    private static final String[] COLUMNAS = {"Identificador", "Nombre", "Teléfono"};

    private final ModeloFuncionarios modelo;
    private final JTextField campoBusqueda = new JTextField(20);
    private final JButton botonBuscar = new JButton("Buscar", Iconos.boton("Buscar"));
    private final ModeloTabla tablaModelo = new ModeloTabla(COLUMNAS);
    private final JTable tabla = new JTable(tablaModelo);
    private final JTextField campoId = new JTextField(12);
    private final JTextField campoNombre = new JTextField(20);
    private final JTextField campoTelefono = new JTextField(12);
    private final JButton botonLimpiar = new JButton("Limpiar", Iconos.boton("Limpiar"));
    private final JButton botonGuardar = new JButton("Guardar", Iconos.boton("Guardar"));
    private final JButton botonEliminar = new JButton("Eliminar", Iconos.boton("Eliminar"));
    private final JButton botonImprimir = new JButton("Imprimir PDF", Iconos.boton("PDF"));

    private Runnable alBuscar;
    private Runnable alGuardar;
    private Runnable alEliminar;
    private Runnable alLimpiar;
    private Runnable alImprimir;
    private IntConsumer alSeleccionarFila;
    private boolean actualizando;

    public VistaFuncionarios(ModeloFuncionarios modelo) {
        this.modelo = modelo;
        setLayout(new BorderLayout());
        add(crearPanelBusqueda(), BorderLayout.NORTH);
        add(crearTabla(), BorderLayout.CENTER);
        add(crearFormulario(), BorderLayout.SOUTH);

        botonBuscar.addActionListener(e -> disparar(alBuscar));
        campoBusqueda.addActionListener(e -> disparar(alBuscar));
        botonGuardar.addActionListener(e -> disparar(alGuardar));
        botonEliminar.addActionListener(e -> disparar(alEliminar));
        botonLimpiar.addActionListener(e -> disparar(alLimpiar));
        botonImprimir.addActionListener(e -> disparar(alImprimir));

        tabla.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting() && !actualizando) {
                int fila = tabla.getSelectedRow();
                if (fila >= 0 && alSeleccionarFila != null) {
                    alSeleccionarFila.accept(fila);
                }
            }
        });

        actualizar();
    }

    private JPanel crearPanelBusqueda() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Buscar:"));
        campoBusqueda.setToolTipText("Identificador o nombre; vacío para listar todos");
        panel.add(campoBusqueda);
        panel.add(botonBuscar);
        return panel;
    }

    private JScrollPane crearTabla() {
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return new JScrollPane(tabla);
    }

    private JPanel crearFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Datos del funcionario"));
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(4, 6, 4, 6);
        restricciones.fill = GridBagConstraints.HORIZONTAL;

        agregarCampo(panel, restricciones, 0, "Identificador:", campoId);
        agregarCampo(panel, restricciones, 1, "Nombre:", campoNombre);
        agregarCampo(panel, restricciones, 2, "Teléfono:", campoTelefono);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botones.add(botonLimpiar);
        botones.add(botonGuardar);
        botones.add(botonEliminar);
        botones.add(botonImprimir);

        restricciones.gridx = 0;
        restricciones.gridy = 3;
        restricciones.gridwidth = 4;
        panel.add(botones, restricciones);
        return panel;
    }

    private static void agregarCampo(JPanel panel, GridBagConstraints restricciones,
                                     int fila, String etiqueta, JTextField campo) {
        restricciones.gridx = 0;
        restricciones.gridy = fila;
        restricciones.gridwidth = 1;
        panel.add(new JLabel(etiqueta), restricciones);
        restricciones.gridx = 1;
        restricciones.gridwidth = 3;
        panel.add(campo, restricciones);
    }

    /** Vuelca el modelo en la tabla y en el formulario. */
    public void actualizar() {
        actualizando = true;
        List<Funcionario> funcionarios = modelo.getFuncionarios();
        tablaModelo.vaciar();
        for (Funcionario funcionario : funcionarios) {
            tablaModelo.addRow(new Object[]{funcionario.getId(), funcionario.getNombre(),
                    funcionario.getTelefono()});
        }
        tabla.clearSelection();
        actualizando = false;
        mostrarSeleccion();
    }

    /** Refleja en el formulario la fila seleccionada del modelo. */
    public void mostrarSeleccion() {
        Funcionario seleccionado = modelo.getSeleccionado();
        if (seleccionado == null) {
            limpiarFormulario();
            return;
        }
        campoId.setText(seleccionado.getId());
        campoId.setEditable(false);
        campoNombre.setText(seleccionado.getNombre());
        campoTelefono.setText(seleccionado.getTelefono());
    }

    public void limpiarFormulario() {
        campoId.setText("");
        campoId.setEditable(true);
        campoNombre.setText("");
        campoTelefono.setText("");
        tabla.clearSelection();
    }

    public String getCriterio() {
        return campoBusqueda.getText().trim();
    }

    public void establecerCriterio(String criterio) {
        campoBusqueda.setText(criterio == null ? "" : criterio);
    }

    public String getId() {
        return campoId.getText().trim();
    }

    public String getNombre() {
        return campoNombre.getText().trim();
    }

    public String getTelefono() {
        return campoTelefono.getText().trim();
    }

    public void establecerFormulario(String id, String nombre, String telefono) {
        campoId.setText(id == null ? "" : id);
        campoNombre.setText(nombre == null ? "" : nombre);
        campoTelefono.setText(telefono == null ? "" : telefono);
    }

    public int getFilaSeleccionada() {
        return tabla.getSelectedRow();
    }

    public void seleccionarFila(int fila) {
        if (fila >= 0 && fila < tabla.getRowCount()) {
            tabla.setRowSelectionInterval(fila, fila);
        }
    }

    public void habilitar(boolean habilitado) {
        campoBusqueda.setEnabled(habilitado);
        botonBuscar.setEnabled(habilitado);
        campoId.setEnabled(habilitado);
        campoNombre.setEnabled(habilitado);
        campoTelefono.setEnabled(habilitado);
        botonLimpiar.setEnabled(habilitado);
        botonGuardar.setEnabled(habilitado);
        botonEliminar.setEnabled(habilitado);
        botonImprimir.setEnabled(habilitado);
        tabla.setEnabled(habilitado);
    }

    /** Pide el destino del PDF; {@code null} si el usuario canceló. */
    public Path elegirDestinoPdf(String nombreSugerido) {
        return SelectorArchivoPdf.elegir(this, nombreSugerido);
    }

    public boolean confirmarEliminacion(String id) {
        return Dialogos.confirmar(this, "Eliminar funcionario",
                "¿Desea eliminar al funcionario " + id + "?");
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

    public void alBuscar(Runnable accion) {
        alBuscar = accion;
    }

    public void alGuardar(Runnable accion) {
        alGuardar = accion;
    }

    public void alEliminar(Runnable accion) {
        alEliminar = accion;
    }

    public void alLimpiar(Runnable accion) {
        alLimpiar = accion;
    }

    public void alImprimir(Runnable accion) {
        alImprimir = accion;
    }

    public void alSeleccionarFila(IntConsumer accion) {
        alSeleccionarFila = accion;
    }

    public JTable getTabla() {
        return tabla;
    }

    public JButton getBotonGuardar() {
        return botonGuardar;
    }

    private static void disparar(Runnable accion) {
        if (accion != null) {
            accion.run();
        }
    }
}
