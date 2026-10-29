package com.sistema.frontend.view;

import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.frontend.model.ModeloCategorias;
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
 * Vista del módulo de categorías de recursos.
 *
 * <p>El identificador se genera en el backend al crear; al modificar se puede
 * cambiar y los recursos asociados se actualizan en el servidor.</p>
 */
public final class VistaCategorias extends JPanel {

    private static final String TITULO = "Categorías";
    private static final String[] COLUMNAS = {"Identificador", "Descripción"};

    private final ModeloCategorias modelo;
    private final JTextField campoBusqueda = new JTextField(20);
    private final JButton botonBuscar = new JButton("Buscar", Iconos.boton("Buscar"));
    private final ModeloTabla tablaModelo = new ModeloTabla(COLUMNAS);
    private final JTable tabla = new JTable(tablaModelo);
    private final JTextField campoId = new JTextField(12);
    private final JTextField campoDescripcion = new JTextField(30);
    private final JLabel ayudaId = new JLabel("Se genera automáticamente al crear.");
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

    public VistaCategorias(ModeloCategorias modelo) {
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
        panel.add(new JLabel("Buscar por ID o descripción:"));
        campoBusqueda.setToolTipText("Criterio de búsqueda; vacío para listar todas");
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
        panel.setBorder(BorderFactory.createTitledBorder("Datos de la categoría"));
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(4, 6, 4, 6);
        restricciones.fill = GridBagConstraints.HORIZONTAL;

        restricciones.gridx = 0;
        restricciones.gridy = 0;
        panel.add(new JLabel("Identificador:"), restricciones);
        restricciones.gridx = 1;
        restricciones.gridwidth = 2;
        campoId.setEditable(false);
        panel.add(campoId, restricciones);

        restricciones.gridx = 3;
        restricciones.gridwidth = 1;
        panel.add(ayudaId, restricciones);

        restricciones.gridx = 0;
        restricciones.gridy = 1;
        restricciones.gridwidth = 1;
        panel.add(new JLabel("Descripción:"), restricciones);
        restricciones.gridx = 1;
        restricciones.gridwidth = 3;
        panel.add(campoDescripcion, restricciones);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botones.add(botonLimpiar);
        botones.add(botonGuardar);
        botones.add(botonEliminar);
        botones.add(botonImprimir);

        restricciones.gridx = 0;
        restricciones.gridy = 2;
        restricciones.gridwidth = 4;
        panel.add(botones, restricciones);
        return panel;
    }

    /** Vuelca el modelo en la tabla y en el formulario. */
    public void actualizar() {
        actualizando = true;
        List<CategoriaRecurso> categorias = modelo.getCategorias();
        tablaModelo.vaciar();
        for (CategoriaRecurso categoria : categorias) {
            tablaModelo.addRow(new Object[]{categoria.getId(), categoria.getDescripcion()});
        }
        tabla.clearSelection();
        actualizando = false;
        mostrarSeleccion();
    }

    /** Refleja en el formulario la categoría seleccionada. */
    public void mostrarSeleccion() {
        CategoriaRecurso seleccionada = modelo.getSeleccionada();
        if (seleccionada == null) {
            limpiarFormulario();
            return;
        }
        campoId.setText(seleccionada.getId());
        campoId.setEditable(true);
        campoDescripcion.setText(seleccionada.getDescripcion());
        ayudaId.setText("Puede cambiar el identificador.");
    }

    public void limpiarFormulario() {
        campoId.setText("");
        campoId.setEditable(false);
        campoDescripcion.setText("");
        ayudaId.setText("Se genera automáticamente al crear.");
        tabla.clearSelection();
    }

    public String getCriterio() {
        return campoBusqueda.getText().trim();
    }

    public void establecerCriterio(String criterio) {
        campoBusqueda.setText(criterio == null ? "" : criterio);
    }

    public String getIdentificador() {
        return campoId.getText().trim();
    }

    public String getDescripcion() {
        return campoDescripcion.getText().trim();
    }

    public void establecerFormulario(String id, String descripcion) {
        campoId.setText(id == null ? "" : id);
        campoId.setEditable(id != null && !id.isBlank());
        campoDescripcion.setText(descripcion == null ? "" : descripcion);
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
        campoDescripcion.setEnabled(habilitado);
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
        return Dialogos.confirmar(this, "Eliminar categoría",
                "¿Desea eliminar la categoría " + id + "?");
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

    private static void disparar(Runnable accion) {
        if (accion != null) {
            accion.run();
        }
    }
}
