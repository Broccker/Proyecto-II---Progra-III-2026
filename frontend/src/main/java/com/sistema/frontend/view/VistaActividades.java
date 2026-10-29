package com.sistema.frontend.view;

import com.sistema.common.dto.CeldaCalendario;
import com.sistema.common.dto.MatrizActividades;
import com.sistema.frontend.model.ModeloActividades;
import com.sistema.frontend.util.Iconos;
import com.sistema.frontend.view.componentes.ModeloTabla;
import com.sistema.frontend.view.componentes.SelectorArchivoPdf;
import com.sistema.frontend.view.componentes.SelectorFecha;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Vista de la programación semanal de actividades (24 horas por 7 días).
 */
public final class VistaActividades extends JPanel {

    private static final String TITULO = "Actividades";
    private static final Color COLOR_OCUPADO = new Color(0xA0, 0xD8, 0xFF);

    private final ModeloActividades modelo;
    private final SelectorFecha campoFecha = new SelectorFecha();
    private final JButton botonCargar = new JButton("Cargar", Iconos.boton("Cargar"));
    private final JButton botonImprimir = new JButton("Imprimir PDF", Iconos.boton("PDF"));
    private final ModeloTabla tablaModelo = new ModeloTabla();
    private final JTable tabla = new JTable(tablaModelo);

    private Runnable alCargar;
    private Runnable alImprimir;

    public VistaActividades(ModeloActividades modelo) {
        this.modelo = modelo;
        setLayout(new BorderLayout());
        add(crearFiltros(), BorderLayout.NORTH);
        add(crearTabla(), BorderLayout.CENTER);
        add(crearBotones(), BorderLayout.SOUTH);

        botonCargar.addActionListener(e -> disparar(alCargar));
        botonImprimir.addActionListener(e -> disparar(alImprimir));
        actualizar();
    }

    private JPanel crearFiltros() {
        JPanel filtros = new JPanel(new GridBagLayout());
        filtros.setBorder(BorderFactory.createTitledBorder("Filtros"));
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(6, 8, 6, 8);
        restricciones.fill = GridBagConstraints.HORIZONTAL;

        restricciones.gridx = 0;
        restricciones.gridy = 0;
        filtros.add(new JLabel("Fecha de referencia:"), restricciones);
        restricciones.gridx = 1;
        filtros.add(campoFecha, restricciones);
        restricciones.gridx = 2;
        filtros.add(botonCargar, restricciones);
        return filtros;
    }

    private JScrollPane crearTabla() {
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable origen, Object valor,
                                                           boolean seleccionada,
                                                           boolean tieneFoco,
                                                           int fila, int columna) {
                Component componente = super.getTableCellRendererComponent(origen, valor,
                        seleccionada, tieneFoco, fila, columna);
                if (valor instanceof CeldaCalendario celda) {
                    setText(celda.texto());
                    if (seleccionada) {
                        componente.setBackground(origen.getSelectionBackground());
                        componente.setForeground(origen.getSelectionForeground());
                    } else {
                        componente.setBackground(celda.ocupada() ? COLOR_OCUPADO : Color.WHITE);
                        componente.setForeground(Color.BLACK);
                    }
                } else {
                    componente.setBackground(Color.WHITE);
                }
                return componente;
            }
        });
        return new JScrollPane(tabla);
    }

    private JPanel crearBotones() {
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botones.add(botonImprimir);
        return botones;
    }

    /** Vuelca la matriz del modelo en la tabla, si existe. */
    public void actualizar() {
        MatrizActividades matriz = modelo.getMatriz();
        if (matriz != null) {
            llenarTabla(matriz);
        }
    }

    private void llenarTabla(MatrizActividades matriz) {
        List<String> columnas = new ArrayList<>();
        columnas.add("Hora");
        columnas.addAll(matriz.dias());
        tablaModelo.setColumnIdentifiers(columnas.toArray());
        tablaModelo.vaciar();
        for (int fila = 0; fila < matriz.horas().size(); fila++) {
            List<CeldaCalendario> celdas = matriz.celdas().get(fila);
            Object[] valores = new Object[columnas.size()];
            valores[0] = matriz.horas().get(fila).toString();
            for (int columna = 0; columna < celdas.size(); columna++) {
                valores[columna + 1] = celdas.get(columna);
            }
            tablaModelo.addRow(valores);
        }
    }

    public LocalDate getFecha() {
        return campoFecha.getFecha();
    }

    public void establecerFecha(LocalDate fecha) {
        campoFecha.setFecha(fecha);
    }

    /** Pide el destino del PDF; {@code null} si el usuario canceló. */
    public Path elegirDestinoPdf(String nombreSugerido) {
        return SelectorArchivoPdf.elegir(this, nombreSugerido);
    }

    public void habilitar(boolean habilitado) {
        campoFecha.setEnabled(habilitado);
        botonCargar.setEnabled(habilitado);
        botonImprimir.setEnabled(habilitado);
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

    public void alCargar(Runnable accion) {
        alCargar = accion;
    }

    public void alImprimir(Runnable accion) {
        alImprimir = accion;
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
