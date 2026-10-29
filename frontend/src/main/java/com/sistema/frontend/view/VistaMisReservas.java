package com.sistema.frontend.view;

import com.sistema.common.modelo.Reserva;
import com.sistema.frontend.model.ModeloReservas;
import com.sistema.frontend.util.Iconos;
import com.sistema.frontend.view.componentes.ModeloTabla;
import com.sistema.frontend.view.componentes.SelectorArchivoPdf;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

/**
 * Historial de reservas del funcionario, con cancelación y exportación a PDF.
 */
public final class VistaMisReservas extends JPanel {

    private static final String TITULO = "Mis reservas";
    private static final String[] COLUMNAS = {"Id", "Actividad", "Fecha", "Horario",
            "Recursos", "Estado"};

    private final ModeloReservas modelo;
    private final ModeloTabla tablaModelo = new ModeloTabla(COLUMNAS);
    private final JTable tabla = new JTable(tablaModelo);
    private final JButton botonCancelar =
            new JButton("Cancelar reserva", Iconos.boton("CancelarReserva"));
    private final JButton botonImprimir = new JButton("Imprimir PDF", Iconos.boton("PDF"));
    private final JButton botonActualizar =
            new JButton("Actualizar", Iconos.boton("Actualizar"));

    private Runnable alCancelar;
    private Runnable alImprimir;
    private Runnable alActualizar;
    private IntConsumer alSeleccionarFila;
    private boolean actualizando;

    public VistaMisReservas(ModeloReservas modelo) {
        this.modelo = modelo;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Mis reservas"));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botones.add(botonCancelar);
        botones.add(botonImprimir);
        botones.add(botonActualizar);
        add(botones, BorderLayout.SOUTH);

        botonCancelar.addActionListener(e -> disparar(alCancelar));
        botonImprimir.addActionListener(e -> disparar(alImprimir));
        botonActualizar.addActionListener(e -> disparar(alActualizar));

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

    /** Vuelca las reservas del modelo en la tabla. */
    public void actualizar() {
        actualizando = true;
        List<Reserva> reservas = modelo.getReservas();
        tablaModelo.vaciar();
        for (Reserva reserva : reservas) {
            String recursos = reserva.getRecursos().stream()
                    .map(com.sistema.common.modelo.Recurso::getDescripcion)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            tablaModelo.addRow(new Object[]{
                    reserva.getId(),
                    reserva.getActividad(),
                    reserva.getFecha().toString(),
                    reserva.getHoraInicio() + " – " + reserva.getHoraFin(),
                    recursos,
                    reserva.getEstado()
            });
        }
        tabla.clearSelection();
        actualizando = false;
    }

    public int getFilaSeleccionada() {
        return tabla.getSelectedRow();
    }

    public void seleccionarFila(int fila) {
        if (fila >= 0 && fila < tabla.getRowCount()) {
            tabla.setRowSelectionInterval(fila, fila);
        }
    }

    public boolean confirmarCancelacion(String id) {
        return Dialogos.confirmar(this, "Cancelar reserva",
                "¿Desea cancelar la reserva " + id + "?");
    }

    /** Pide el destino del PDF; {@code null} si el usuario canceló. */
    public Path elegirDestinoPdf(String nombreSugerido) {
        return SelectorArchivoPdf.elegir(this, nombreSugerido);
    }

    public void habilitar(boolean habilitado) {
        botonCancelar.setEnabled(habilitado);
        botonImprimir.setEnabled(habilitado);
        botonActualizar.setEnabled(habilitado);
        tabla.setEnabled(habilitado);
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

    public void alCancelar(Runnable accion) {
        alCancelar = accion;
    }

    public void alImprimir(Runnable accion) {
        alImprimir = accion;
    }

    public void alActualizar(Runnable accion) {
        alActualizar = accion;
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
