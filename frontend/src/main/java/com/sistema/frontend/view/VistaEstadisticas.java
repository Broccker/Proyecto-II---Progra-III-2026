package com.sistema.frontend.view;

import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import com.sistema.frontend.model.ModeloEstadisticas;
import com.sistema.frontend.util.Iconos;
import com.sistema.frontend.view.componentes.SelectorArchivoPdf;
import com.sistema.frontend.view.componentes.SelectorFecha;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import org.jfree.chart.JFreeChart;

/**
 * Vista de estadísticas con dos paneles: recursos usados por categoría y
 * actividades realizadas por semana.
 *
 * <p>Cada panel tiene su propio rango de fechas, botón de confirmar, tabla y
 * gráfico de barras. Los datos llegan ya agregados por el backend; aquí solo
 * se formatean y se pintan.</p>
 */
public final class VistaEstadisticas extends JPanel {

    private static final String TITULO = "Estadísticas";
    private static final String[] COLUMNAS_RECURSOS = {"Categoría", "Reservas"};
    private static final String[] COLUMNAS_ACTIVIDADES = {"Semana (lunes)", "Actividades"};

    private final ModeloEstadisticas modelo;
    private final PanelEstadistica panelRecursos =
            new PanelEstadistica("Categorías reservadas", COLUMNAS_RECURSOS[0],
                    COLUMNAS_RECURSOS[1]);
    private final PanelEstadistica panelActividades =
            new PanelEstadistica("Actividades por semana", COLUMNAS_ACTIVIDADES[0],
                    COLUMNAS_ACTIVIDADES[1]);

    private Runnable alCalcularRecursos;
    private Runnable alCalcularActividades;
    private Runnable alImprimirRecursos;
    private Runnable alImprimirActividades;

    public VistaEstadisticas(ModeloEstadisticas modelo) {
        this.modelo = modelo;
        setLayout(new BorderLayout());

        panelRecursos.alCalcular(() -> disparar(alCalcularRecursos));
        panelRecursos.alImprimir(() -> disparar(alImprimirRecursos));
        panelActividades.alCalcular(() -> disparar(alCalcularActividades));
        panelActividades.alImprimir(() -> disparar(alImprimirActividades));

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Recursos", Iconos.pestana("Estadisticas"), panelRecursos);
        pestanas.addTab("Actividades", Iconos.pestana("Actividades"), panelActividades);
        add(pestanas, BorderLayout.CENTER);

        actualizar();
    }

    /** Vuelca en ambos paneles los resultados del modelo. */
    public void actualizar() {
        List<ConteoCategoria> categorias = modelo.getCategorias();
        panelRecursos.mostrar(
                categorias.stream().map(conteo -> conteo.categoria().getDescripcion()).toList(),
                categorias.stream().map(ConteoCategoria::cantidad).toList());

        List<ConteoSemana> semanas = modelo.getSemanas();
        panelActividades.mostrar(
                semanas.stream().map(conteo -> conteo.lunes().format(SelectorFecha.FORMATO)).toList(),
                semanas.stream().map(ConteoSemana::cantidad).toList());
    }

    public LocalDate getDesdeRecursos() {
        return panelRecursos.getDesde();
    }

    public LocalDate getHastaRecursos() {
        return panelRecursos.getHasta();
    }

    public void setRangoRecursos(LocalDate desde, LocalDate hasta) {
        panelRecursos.setRango(desde, hasta);
    }

    public LocalDate getDesdeActividades() {
        return panelActividades.getDesde();
    }

    public LocalDate getHastaActividades() {
        return panelActividades.getHasta();
    }

    public void setRangoActividades(LocalDate desde, LocalDate hasta) {
        panelActividades.setRango(desde, hasta);
    }

    public void habilitarRecursos(boolean habilitado) {
        panelRecursos.habilitar(habilitado);
    }

    public void habilitarActividades(boolean habilitado) {
        panelActividades.habilitar(habilitado);
    }

    public void habilitar(boolean habilitado) {
        habilitarRecursos(habilitado);
        habilitarActividades(habilitado);
    }

    /** Pide el destino del PDF; {@code null} si el usuario canceló. */
    public Path elegirDestinoPdf(String nombreSugerido) {
        return SelectorArchivoPdf.elegir(this, nombreSugerido);
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

    public void alCalcularRecursos(Runnable accion) {
        alCalcularRecursos = accion;
    }

    public void alCalcularActividades(Runnable accion) {
        alCalcularActividades = accion;
    }

    public void alImprimirRecursos(Runnable accion) {
        alImprimirRecursos = accion;
    }

    public void alImprimirActividades(Runnable accion) {
        alImprimirActividades = accion;
    }

    public JTable getTablaRecursos() {
        return panelRecursos.getTabla();
    }

    public JTable getTablaActividades() {
        return panelActividades.getTabla();
    }

    public JFreeChart getGraficoRecursos() {
        return panelRecursos.getGrafico();
    }

    public JFreeChart getGraficoActividades() {
        return panelActividades.getGrafico();
    }

    JButton getBotonCalcularRecursos() {
        return panelRecursos.getBotonCalcular();
    }

    JButton getBotonCalcularActividades() {
        return panelActividades.getBotonCalcular();
    }

    private static void disparar(Runnable accion) {
        if (accion != null) {
            accion.run();
        }
    }
}
