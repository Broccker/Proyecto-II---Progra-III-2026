package com.sistema.frontend.view;

import com.sistema.frontend.util.Iconos;
import com.sistema.frontend.view.componentes.ModeloTabla;
import com.sistema.frontend.view.componentes.SelectorFecha;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Panel de una estadística: rango de fechas, tabla de resultados y gráfico de
 * barras.
 *
 * <p>No consulta al backend ni calcula nada: recibe las etiquetas y cantidades
 * ya agregadas y se limita a pintarlas. Los botones se exponen al controlador
 * como acciones.</p>
 */
final class PanelEstadistica extends JPanel {

    private final String titulo;
    private final SelectorFecha campoDesde = new SelectorFecha(LocalDate.now().minusMonths(1));
    private final SelectorFecha campoHasta = new SelectorFecha(LocalDate.now());
    private final JButton botonCalcular = new JButton("Calcular", Iconos.boton("Calcular"));
    private final JButton botonImprimir = new JButton("Imprimir PDF", Iconos.boton("PDF"));
    private final ModeloTabla tablaModelo;
    private final JTable tabla;
    private final ChartPanel panelGrafico = new ChartPanel(null);

    private Runnable alCalcular;
    private Runnable alImprimir;

    PanelEstadistica(String titulo, String columnaEtiqueta, String columnaCantidad) {
        this.titulo = titulo;
        this.tablaModelo = new ModeloTabla(columnaEtiqueta, columnaCantidad);
        this.tabla = new JTable(tablaModelo);

        setLayout(new BorderLayout());
        add(crearFiltros(), BorderLayout.NORTH);
        add(crearContenido(), BorderLayout.CENTER);

        botonCalcular.addActionListener(evento -> disparar(alCalcular));
        botonImprimir.addActionListener(evento -> disparar(alImprimir));
    }

    private JPanel crearFiltros() {
        JPanel filtros = new JPanel(new GridBagLayout());
        filtros.setBorder(BorderFactory.createTitledBorder(titulo));
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(6, 8, 6, 8);
        restricciones.fill = GridBagConstraints.HORIZONTAL;

        restricciones.gridx = 0;
        restricciones.gridy = 0;
        filtros.add(new JLabel("Desde:"), restricciones);
        restricciones.gridx = 1;
        filtros.add(campoDesde, restricciones);

        restricciones.gridx = 2;
        filtros.add(new JLabel("Hasta:"), restricciones);
        restricciones.gridx = 3;
        filtros.add(campoHasta, restricciones);

        restricciones.gridx = 4;
        filtros.add(botonCalcular, restricciones);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.add(filtros, BorderLayout.WEST);
        JPanel inferior = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inferior.add(botonImprimir);
        contenedor.add(inferior, BorderLayout.EAST);
        return contenedor;
    }

    private JSplitPane crearContenido() {
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        panelGrafico.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                panelGrafico, new JScrollPane(tabla));
        division.setResizeWeight(0.55);
        return division;
    }

    /** Pinta los resultados ya agregados y reconstruye el gráfico de barras. */
    void mostrar(List<String> etiquetas, List<Long> cantidades) {
        tablaModelo.vaciar();
        DefaultCategoryDataset datos = new DefaultCategoryDataset();
        for (int indice = 0; indice < etiquetas.size(); indice++) {
            long cantidad = cantidades.get(indice);
            tablaModelo.addRow(new Object[]{etiquetas.get(indice), cantidad});
            datos.addValue(cantidad, "Cantidad", etiquetas.get(indice));
        }
        panelGrafico.setChart(crearGrafico(datos));
    }

    private JFreeChart crearGrafico(DefaultCategoryDataset datos) {
        return ChartFactory.createBarChart(titulo, "", "Cantidad", datos,
                PlotOrientation.VERTICAL, false, true, false);
    }

    LocalDate getDesde() {
        return campoDesde.getFecha();
    }

    LocalDate getHasta() {
        return campoHasta.getFecha();
    }

    void setRango(LocalDate desde, LocalDate hasta) {
        campoDesde.setFecha(desde);
        campoHasta.setFecha(hasta);
    }

    void habilitar(boolean habilitado) {
        campoDesde.setEnabled(habilitado);
        campoHasta.setEnabled(habilitado);
        botonCalcular.setEnabled(habilitado);
        botonImprimir.setEnabled(habilitado);
        tabla.setEnabled(habilitado);
    }

    void alCalcular(Runnable accion) {
        alCalcular = accion;
    }

    void alImprimir(Runnable accion) {
        alImprimir = accion;
    }

    JTable getTabla() {
        return tabla;
    }

    JFreeChart getGrafico() {
        return panelGrafico.getChart();
    }

    JButton getBotonCalcular() {
        return botonCalcular;
    }

    private static void disparar(Runnable accion) {
        if (accion != null) {
            accion.run();
        }
    }
}
