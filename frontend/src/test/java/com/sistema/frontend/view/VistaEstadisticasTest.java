package com.sistema.frontend.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.frontend.model.ModeloEstadisticas;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTabbedPane;
import org.jfree.data.category.CategoryDataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VistaEstadisticasTest {

    private ModeloEstadisticas modelo;
    private VistaEstadisticas vista;

    @BeforeEach
    void preparar() {
        modelo = new ModeloEstadisticas();
        vista = new VistaEstadisticas(modelo);
    }

    private void cargarDatos() {
        modelo.establecerCategorias(java.util.List.of(
                new ConteoCategoria(new CategoriaRecurso("CAT-000001", "Proyectores"), 5),
                new ConteoCategoria(new CategoriaRecurso("CAT-000002", "Salas"), 2)));
        modelo.establecerSemanas(java.util.List.of(
                new ConteoSemana(LocalDate.of(2026, 11, 2), 3),
                new ConteoSemana(LocalDate.of(2026, 11, 9), 1)));
        vista.actualizar();
    }

    @Test
    @DisplayName("La vista tiene los paneles de Recursos y Actividades")
    void tieneLosDosPaneles() {
        JTabbedPane pestanas = (JTabbedPane) vista.getComponent(0);

        assertEquals(2, pestanas.getTabCount());
        assertEquals("Recursos", pestanas.getTitleAt(0));
        assertEquals("Actividades", pestanas.getTitleAt(1));
    }

    @Test
    @DisplayName("El panel de recursos muestra categoría y cantidad")
    void muestraLosRecursos() {
        cargarDatos();

        assertEquals(2, vista.getTablaRecursos().getRowCount());
        assertEquals("Categoría", vista.getTablaRecursos().getColumnName(0));
        assertEquals("Reservas", vista.getTablaRecursos().getColumnName(1));
        assertEquals("Proyectores", vista.getTablaRecursos().getValueAt(0, 0));
        assertEquals(5L, vista.getTablaRecursos().getValueAt(0, 1));
    }

    @Test
    @DisplayName("El gráfico de recursos representa las mismas cantidades")
    void graficaLosRecursos() {
        cargarDatos();

        assertNotNull(vista.getGraficoRecursos());
        CategoryDataset datos = vista.getGraficoRecursos().getCategoryPlot().getDataset();
        assertEquals(5.0, datos.getValue("Cantidad", "Proyectores").doubleValue());
        assertEquals(2.0, datos.getValue("Cantidad", "Salas").doubleValue());
    }

    @Test
    @DisplayName("El panel de actividades muestra la semana y su cantidad")
    void muestraLasActividades() {
        cargarDatos();

        assertEquals(2, vista.getTablaActividades().getRowCount());
        assertEquals("Semana (lunes)", vista.getTablaActividades().getColumnName(0));
        assertEquals("Actividades", vista.getTablaActividades().getColumnName(1));
        assertEquals("02/11/2026", vista.getTablaActividades().getValueAt(0, 0));
        assertEquals(3L, vista.getTablaActividades().getValueAt(0, 1));
    }

    @Test
    @DisplayName("El gráfico de actividades representa las mismas cantidades")
    void graficaLasActividades() {
        cargarDatos();

        assertNotNull(vista.getGraficoActividades());
        CategoryDataset datos = vista.getGraficoActividades().getCategoryPlot().getDataset();
        assertEquals(3.0, datos.getValue("Cantidad", "02/11/2026").doubleValue());
        assertEquals(1.0, datos.getValue("Cantidad", "09/11/2026").doubleValue());
    }

    @Test
    @DisplayName("Sin resultados las tablas quedan vacías y el gráfico existe")
    void vaciaSinResultados() {
        assertEquals(0, vista.getTablaRecursos().getRowCount());
        assertEquals(0, vista.getTablaActividades().getRowCount());
        assertNotNull(vista.getGraficoRecursos());
        assertNotNull(vista.getGraficoActividades());
    }

    @Test
    @DisplayName("Cada panel tiene su propio rango de fechas")
    void guardaCadaRango() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 2, 1);
        LocalDate desdeActividades = LocalDate.of(2026, 3, 1);
        LocalDate hastaActividades = LocalDate.of(2026, 4, 1);

        vista.setRangoRecursos(desde, hasta);
        vista.setRangoActividades(desdeActividades, hastaActividades);

        assertEquals(desde, vista.getDesdeRecursos());
        assertEquals(hasta, vista.getHastaRecursos());
        assertEquals(desdeActividades, vista.getDesdeActividades());
        assertEquals(hastaActividades, vista.getHastaActividades());
    }

    @Test
    @DisplayName("El botón Calcular de cada panel avisa a su acción")
    void losBotonesAvisanAlControlador() {
        AtomicInteger recursos = new AtomicInteger();
        AtomicInteger actividades = new AtomicInteger();
        vista.alCalcularRecursos(recursos::incrementAndGet);
        vista.alCalcularActividades(actividades::incrementAndGet);

        vista.getBotonCalcularRecursos().doClick();
        vista.getBotonCalcularActividades().doClick();

        assertEquals(1, recursos.get());
        assertEquals(1, actividades.get());
    }

    @Test
    @DisplayName("Habilitar y deshabilitar afecta a los controles de ambos paneles")
    void habilitaLosPaneles() {
        vista.habilitar(false);
        assertFalse(vista.getBotonCalcularRecursos().isEnabled());
        assertFalse(vista.getBotonCalcularActividades().isEnabled());

        vista.habilitar(true);
        assertTrue(vista.getBotonCalcularRecursos().isEnabled());
        assertTrue(vista.getBotonCalcularActividades().isEnabled());
    }
}
