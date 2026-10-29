package com.sistema.frontend.view;

import com.sistema.common.dto.DatosReservaSugerida;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.frontend.model.ModeloReservas;
import com.sistema.frontend.util.Iconos;
import com.sistema.frontend.view.componentes.SelectorFecha;
import com.sistema.frontend.view.componentes.SelectorHora;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Formulario de alta de una reserva, con extracción de datos mediante IA.
 */
public final class VistaNuevaReserva extends JPanel {

    private static final String TITULO = "Nueva reserva";

    private final ModeloReservas modelo;
    private final JTextField campoFrase = new JTextField(32);
    private final JTextField campoActividad = new JTextField(28);
    private final SelectorFecha campoFecha = new SelectorFecha();
    private final SelectorHora campoHoraInicio = new SelectorHora(LocalTime.of(9, 0));
    private final SelectorHora campoHoraFin = new SelectorHora(LocalTime.of(10, 0));
    private final DefaultListModel<CategoriaRecurso> modeloCategorias = new DefaultListModel<>();
    private final JList<CategoriaRecurso> listaCategorias = new JList<>(modeloCategorias);
    private final JButton botonExtraer = new JButton("Extraer con IA", Iconos.boton("IA"));
    private final JButton botonReservar = new JButton("Reservar", Iconos.boton("Reservar"));
    private final JButton botonLimpiar = new JButton("Limpiar", Iconos.boton("Limpiar"));

    private Runnable alReservar;
    private Runnable alExtraer;
    private Runnable alLimpiar;

    public VistaNuevaReserva(ModeloReservas modelo) {
        this.modelo = modelo;
        setLayout(new BorderLayout());
        add(crearFormulario(), BorderLayout.NORTH);
        add(crearCategorias(), BorderLayout.CENTER);
        add(crearBotones(), BorderLayout.SOUTH);

        botonExtraer.addActionListener(e -> disparar(alExtraer));
        botonReservar.addActionListener(e -> disparar(alReservar));
        botonLimpiar.addActionListener(e -> disparar(alLimpiar));
        actualizar();
    }

    private JPanel crearFormulario() {
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createTitledBorder("Nueva reserva"));
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(5, 7, 5, 7);
        restricciones.fill = GridBagConstraints.HORIZONTAL;

        restricciones.gridx = 0;
        restricciones.gridy = 0;
        formulario.add(new JLabel("Frase con IA:"), restricciones);
        restricciones.gridx = 1;
        restricciones.gridwidth = 2;
        campoFrase.setToolTipText(
                "Ej.: 'Reunión de junta mañana de 9 a 11 con proyector y computadora'");
        formulario.add(campoFrase, restricciones);
        restricciones.gridx = 3;
        restricciones.gridwidth = 1;
        formulario.add(botonExtraer, restricciones);

        restricciones.gridx = 0;
        restricciones.gridy = 1;
        formulario.add(new JLabel("Actividad:"), restricciones);
        restricciones.gridx = 1;
        restricciones.gridwidth = 3;
        formulario.add(campoActividad, restricciones);

        restricciones.gridx = 0;
        restricciones.gridy = 2;
        restricciones.gridwidth = 1;
        formulario.add(new JLabel("Fecha:"), restricciones);
        restricciones.gridx = 1;
        formulario.add(campoFecha, restricciones);

        restricciones.gridx = 2;
        formulario.add(new JLabel("Hora inicio:"), restricciones);
        restricciones.gridx = 3;
        formulario.add(campoHoraInicio, restricciones);

        restricciones.gridx = 2;
        restricciones.gridy = 3;
        formulario.add(new JLabel("Hora fin:"), restricciones);
        restricciones.gridx = 3;
        formulario.add(campoHoraFin, restricciones);
        return formulario;
    }

    private JPanel crearCategorias() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Categorías"));
        listaCategorias.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listaCategorias.setVisibleRowCount(5);
        panel.add(new JScrollPane(listaCategorias), BorderLayout.CENTER);
        panel.add(new JLabel("Seleccione una o más categorías (Ctrl+clic). "
                + "Se asigna el primer recurso disponible de cada una.", JLabel.CENTER),
                BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearBotones() {
        JPanel botones = new JPanel();
        botones.add(botonReservar);
        botones.add(botonLimpiar);
        return botones;
    }

    /** Vuelca las categorías del modelo conservando la selección actual. */
    public void actualizar() {
        List<String> seleccionadas = getIdsCategoriasSeleccionadas();
        modeloCategorias.clear();
        for (CategoriaRecurso categoria : modelo.getCategorias()) {
            modeloCategorias.addElement(categoria);
        }
        seleccionarCategorias(seleccionadas);
    }

    public void seleccionarCategorias(List<String> ids) {
        listaCategorias.clearSelection();
        if (ids == null) {
            return;
        }
        for (int i = 0; i < modeloCategorias.size(); i++) {
            if (ids.contains(modeloCategorias.getElementAt(i).getId())) {
                listaCategorias.addSelectionInterval(i, i);
            }
        }
    }

    public List<String> getIdsCategoriasSeleccionadas() {
        List<String> ids = new ArrayList<>();
        for (CategoriaRecurso categoria : listaCategorias.getSelectedValuesList()) {
            ids.add(categoria.getId());
        }
        return ids;
    }

    public String getActividad() {
        return campoActividad.getText().trim();
    }

    public String getFrase() {
        return campoFrase.getText().trim();
    }

    public LocalDate getFecha() {
        return campoFecha.getFecha();
    }

    public LocalTime getHoraInicio() {
        return campoHoraInicio.getHora();
    }

    public LocalTime getHoraFin() {
        return campoHoraFin.getHora();
    }

    public void establecerActividad(String actividad) {
        campoActividad.setText(actividad == null ? "" : actividad);
    }

    public void establecerFrase(String frase) {
        campoFrase.setText(frase == null ? "" : frase);
    }

    public void establecerFecha(LocalDate fecha) {
        campoFecha.setFecha(fecha);
    }

    public void establecerHorario(LocalTime inicio, LocalTime fin) {
        campoHoraInicio.setHora(inicio);
        campoHoraFin.setHora(fin);
    }

    /** Rellena el formulario con los datos sugeridos por la IA. */
    public void rellenar(DatosReservaSugerida datos) {
        establecerActividad(datos.actividad());
        establecerFecha(datos.fecha());
        establecerHorario(datos.horaInicio(), datos.horaFin());
        seleccionarCategorias(datos.categorias().stream()
                .map(CategoriaRecurso::getId)
                .toList());
    }

    public void limpiar() {
        campoFrase.setText("");
        campoActividad.setText("");
        campoFecha.setFecha(LocalDate.now());
        campoHoraInicio.setHora(LocalTime.of(9, 0));
        campoHoraFin.setHora(LocalTime.of(10, 0));
        listaCategorias.clearSelection();
    }

    public void habilitar(boolean habilitado) {
        campoFrase.setEnabled(habilitado);
        campoActividad.setEnabled(habilitado);
        campoFecha.setEnabled(habilitado);
        campoHoraInicio.setEnabled(habilitado);
        campoHoraFin.setEnabled(habilitado);
        listaCategorias.setEnabled(habilitado);
        botonExtraer.setEnabled(habilitado);
        botonReservar.setEnabled(habilitado);
        botonLimpiar.setEnabled(habilitado);
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

    public void alReservar(Runnable accion) {
        alReservar = accion;
    }

    public void alExtraer(Runnable accion) {
        alExtraer = accion;
    }

    public void alLimpiar(Runnable accion) {
        alLimpiar = accion;
    }

    public JList<CategoriaRecurso> getListaCategorias() {
        return listaCategorias;
    }

    private static void disparar(Runnable accion) {
        if (accion != null) {
            accion.run();
        }
    }
}
