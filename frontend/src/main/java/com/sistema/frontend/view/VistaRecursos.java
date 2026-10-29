package com.sistema.frontend.view;

import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Recurso;
import com.sistema.frontend.model.ModeloRecursos;
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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Vista del módulo de recursos.
 *
 * <p>Permite filtrar por categoría y descripción, y mantener el formulario de
 * alta o modificación. El estado vive en {@link ModeloRecursos}.</p>
 */
public final class VistaRecursos extends JPanel {

    private static final String TITULO = "Recursos";
    private static final String[] COLUMNAS = {"Identificador", "Categoría", "Descripción"};
    private static final String TODAS = "Todas las categorías";

    private final ModeloRecursos modelo;
    private final JComboBox<FiltroCategoria> comboFiltro = new JComboBox<>();
    private final JTextField campoBusqueda = new JTextField(18);
    private final JButton botonBuscar = new JButton("Buscar", Iconos.boton("Buscar"));
    private final ModeloTabla tablaModelo = new ModeloTabla(COLUMNAS);
    private final JTable tabla = new JTable(tablaModelo);
    private final JTextField campoId = new JTextField(12);
    private final JComboBox<CategoriaRecurso> comboCategoria = new JComboBox<>();
    private final JTextField campoDescripcion = new JTextField(30);
    private final JButton botonLimpiar = new JButton("Limpiar", Iconos.boton("Limpiar"));
    private final JButton botonGuardar = new JButton("Guardar", Iconos.boton("Guardar"));
    private final JButton botonEliminar = new JButton("Eliminar", Iconos.boton("Eliminar"));
    private final JButton botonImprimir = new JButton("Imprimir PDF", Iconos.boton("PDF"));

    private Runnable alBuscar;
    private Runnable alGuardar;
    private Runnable alEliminar;
    private Runnable alLimpiar;
    private Runnable alImprimir;
    private Runnable alCambiarFiltro;
    private IntConsumer alSeleccionarFila;
    private boolean actualizando;

    public VistaRecursos(ModeloRecursos modelo) {
        this.modelo = modelo;
        setLayout(new BorderLayout());
        add(crearPanelFiltro(), BorderLayout.NORTH);
        add(crearTabla(), BorderLayout.CENTER);
        add(crearFormulario(), BorderLayout.SOUTH);

        botonBuscar.addActionListener(e -> disparar(alBuscar));
        campoBusqueda.addActionListener(e -> disparar(alBuscar));
        botonGuardar.addActionListener(e -> disparar(alGuardar));
        botonEliminar.addActionListener(e -> disparar(alEliminar));
        botonLimpiar.addActionListener(e -> disparar(alLimpiar));
        botonImprimir.addActionListener(e -> disparar(alImprimir));
        comboFiltro.addActionListener(e -> {
            if (!actualizando) {
                disparar(alCambiarFiltro);
            }
        });

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

    private JPanel crearPanelFiltro() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Categoría:"));
        comboFiltro.setPreferredSize(new java.awt.Dimension(190, 26));
        panel.add(comboFiltro);
        panel.add(new JLabel("Descripción:"));
        campoBusqueda.setToolTipText("Identificador, descripción o categoría");
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
        panel.setBorder(BorderFactory.createTitledBorder("Datos del recurso"));
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(4, 6, 4, 6);
        restricciones.fill = GridBagConstraints.HORIZONTAL;

        agregarCampo(panel, restricciones, 0, "Identificador:", campoId);
        comboCategoria.setPreferredSize(new java.awt.Dimension(220, 26));
        agregarCampo(panel, restricciones, 1, "Categoría:", comboCategoria);
        agregarCampo(panel, restricciones, 2, "Descripción:", campoDescripcion);

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
                                     int fila, String etiqueta, java.awt.Component campo) {
        restricciones.gridx = 0;
        restricciones.gridy = fila;
        restricciones.gridwidth = 1;
        panel.add(new JLabel(etiqueta), restricciones);
        restricciones.gridx = 1;
        restricciones.gridwidth = 3;
        panel.add(campo, restricciones);
    }

    /** Vuelca categorías, filtros, tabla y formulario desde el modelo. */
    public void actualizar() {
        actualizando = true;
        String filtroActual = getFiltroCategoriaId();
        String categoriaFormulario = getCategoriaId();

        comboFiltro.removeAllItems();
        comboFiltro.addItem(new FiltroCategoria(null, TODAS));
        for (CategoriaRecurso categoria : modelo.getCategorias()) {
            comboFiltro.addItem(new FiltroCategoria(categoria.getId(),
                    categoria.getDescripcion()));
        }
        seleccionarFiltro(filtroActual == null ? modelo.getCategoriaFiltroId() : filtroActual);

        List<CategoriaRecurso> categorias = modelo.getCategorias();
        comboCategoria.removeAllItems();
        for (CategoriaRecurso categoria : categorias) {
            comboCategoria.addItem(categoria);
        }
        seleccionarCategoria(categoriaFormulario);

        tablaModelo.vaciar();
        for (Recurso recurso : modelo.getRecursos()) {
            String categoria = recurso.getCategoria() == null
                    ? "" : recurso.getCategoria().getDescripcion();
            tablaModelo.addRow(new Object[]{recurso.getId(), categoria,
                    recurso.getDescripcion()});
        }
        tabla.clearSelection();
        actualizando = false;
        mostrarSeleccion();
    }

    /** Refleja en el formulario el recurso seleccionado. */
    public void mostrarSeleccion() {
        Recurso seleccionado = modelo.getSeleccionado();
        if (seleccionado == null) {
            limpiarFormulario();
            return;
        }
        campoId.setText(seleccionado.getId());
        campoId.setEditable(true);
        campoDescripcion.setText(seleccionado.getDescripcion());
        if (seleccionado.getCategoria() != null) {
            seleccionarCategoria(seleccionado.getCategoria().getId());
        }
    }

    public void limpiarFormulario() {
        campoId.setText("");
        campoId.setEditable(true);
        campoDescripcion.setText("");
        if (comboCategoria.getItemCount() > 0) {
            comboCategoria.setSelectedIndex(0);
        }
        tabla.clearSelection();
    }

    private void seleccionarFiltro(String categoriaId) {
        for (int i = 0; i < comboFiltro.getItemCount(); i++) {
            FiltroCategoria item = comboFiltro.getItemAt(i);
            if (item.id() == null ? categoriaId == null : item.id().equals(categoriaId)) {
                comboFiltro.setSelectedIndex(i);
                return;
            }
        }
        if (comboFiltro.getItemCount() > 0) {
            comboFiltro.setSelectedIndex(0);
        }
    }

    private void seleccionarCategoria(String categoriaId) {
        if (categoriaId == null) {
            return;
        }
        for (int i = 0; i < comboCategoria.getItemCount(); i++) {
            if (comboCategoria.getItemAt(i).getId().equals(categoriaId)) {
                comboCategoria.setSelectedIndex(i);
                return;
            }
        }
    }

    public String getCriterio() {
        return campoBusqueda.getText().trim();
    }

    public void establecerCriterio(String criterio) {
        campoBusqueda.setText(criterio == null ? "" : criterio);
    }

    /** Identificador de la categoría del filtro, o {@code null} para todas. */
    public String getFiltroCategoriaId() {
        FiltroCategoria filtro = (FiltroCategoria) comboFiltro.getSelectedItem();
        return filtro == null ? null : filtro.id();
    }

    public void establecerFiltroCategoria(String categoriaId) {
        seleccionarFiltro(categoriaId);
    }

    public String getIdentificador() {
        return campoId.getText().trim();
    }

    public String getDescripcion() {
        return campoDescripcion.getText().trim();
    }

    /** Identificador de la categoría elegida en el formulario. */
    public String getCategoriaId() {
        CategoriaRecurso categoria = (CategoriaRecurso) comboCategoria.getSelectedItem();
        return categoria == null ? null : categoria.getId();
    }

    public void establecerCategoria(String categoriaId) {
        seleccionarCategoria(categoriaId);
    }

    public void establecerFormulario(String id, String categoriaId, String descripcion) {
        campoId.setText(id == null ? "" : id);
        campoId.setEditable(true);
        seleccionarCategoria(categoriaId);
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
        comboFiltro.setEnabled(habilitado);
        campoBusqueda.setEnabled(habilitado);
        botonBuscar.setEnabled(habilitado);
        campoId.setEnabled(habilitado);
        comboCategoria.setEnabled(habilitado);
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
        return Dialogos.confirmar(this, "Eliminar recurso",
                "¿Desea eliminar el recurso " + id + "?");
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

    public void alCambiarFiltro(Runnable accion) {
        alCambiarFiltro = accion;
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

    /** Elemento del filtro; {@code id == null} representa todas las categorías. */
    private record FiltroCategoria(String id, String descripcion) {
        @Override
        public String toString() {
            return descripcion;
        }
    }
}
