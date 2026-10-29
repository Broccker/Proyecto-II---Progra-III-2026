package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import com.sistema.common.excepciones.SistemaException;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloEstadisticas;
import com.sistema.frontend.view.VistaEstadisticas;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jfree.data.category.CategoryDataset;

class ControladorEstadisticasTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);

    private Service servicio;

    @BeforeEach
    void preparar() {
        servicio = mock(Service.class);
    }

    private static List<ConteoCategoria> categorias() {
        return List.of(new ConteoCategoria(DatosControladores.categoria(), 5),
                new ConteoCategoria(DatosControladores.otraCategoria(), 2));
    }

    private static List<ConteoSemana> semanas() {
        return List.of(new ConteoSemana(LocalDate.of(2026, 10, 5), 3));
    }

    @Test
    @DisplayName("Cargar consulta ambos paneles y pinta tabla y gráfico")
    void cargaAmbosPaneles() {
        when(servicio.categoriasReservadas(any(), any())).thenReturn(categorias());
        when(servicio.actividadesPorSemana(any(), any())).thenReturn(semanas());
        ModeloEstadisticas modelo = new ModeloEstadisticas();
        VistaEstadisticas vista = new VistaEstadisticas(modelo);
        ControladorEstadisticas controlador = new ControladorEstadisticas(servicio, modelo,
                vista, new EjecutorDirecto());

        controlador.cargar();

        assertEquals(2, modelo.getCategorias().size());
        assertEquals(1, modelo.getSemanas().size());
        assertEquals(2, vista.getTablaRecursos().getRowCount());
        assertEquals(1, vista.getTablaActividades().getRowCount());
        assertEquals("Proyectores", vista.getTablaRecursos().getValueAt(0, 0));
        CategoryDataset datos = vista.getGraficoRecursos().getCategoryPlot().getDataset();
        assertEquals(5.0, datos.getValue("Cantidad", "Proyectores").doubleValue());
    }

    @Test
    @DisplayName("Los recursos se consultan con el rango elegido en su panel")
    void consultaRecursosConSuRango() {
        when(servicio.categoriasReservadas(DESDE, HASTA)).thenReturn(categorias());
        VistaEstadisticas vista = new VistaEstadisticas(new ModeloEstadisticas());
        vista.setRangoRecursos(DESDE, HASTA);
        ControladorEstadisticas controlador = new ControladorEstadisticas(servicio,
                new ModeloEstadisticas(), vista, new EjecutorDirecto());

        controlador.consultarRecursos();

        verify(servicio).categoriasReservadas(DESDE, HASTA);
    }

    @Test
    @DisplayName("Las actividades se consultan con el rango elegido en su panel")
    void consultaActividadesConSuRango() {
        when(servicio.actividadesPorSemana(DESDE, HASTA)).thenReturn(semanas());
        VistaEstadisticas vista = new VistaEstadisticas(new ModeloEstadisticas());
        vista.setRangoActividades(DESDE, HASTA);
        ControladorEstadisticas controlador = new ControladorEstadisticas(servicio,
                new ModeloEstadisticas(), vista, new EjecutorDirecto());

        controlador.consultarActividades();

        verify(servicio).actividadesPorSemana(DESDE, HASTA);
    }

    @Test
    @DisplayName("Un rango invertido se avisa y no consulta al backend")
    void rechazaElRangoInvertido() {
        VistaEstadisticas vista = mock(VistaEstadisticas.class);
        when(vista.getDesdeRecursos()).thenReturn(HASTA);
        when(vista.getHastaRecursos()).thenReturn(DESDE);
        ControladorEstadisticas controlador = new ControladorEstadisticas(servicio,
                new ModeloEstadisticas(), vista, new EjecutorDirecto());

        controlador.consultarRecursos();

        verify(vista).mostrarAdvertencia(contains("posterior"));
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Una fecha con formato inválido se avisa y no consulta")
    void rechazaLaFechaInvalida() {
        VistaEstadisticas vista = mock(VistaEstadisticas.class);
        when(vista.getDesdeActividades())
                .thenThrow(new IllegalArgumentException("Formato de fecha inválido."));
        ControladorEstadisticas controlador = new ControladorEstadisticas(servicio,
                new ModeloEstadisticas(), vista, new EjecutorDirecto());

        controlador.consultarActividades();

        verify(vista).mostrarAdvertencia("Formato de fecha inválido.");
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Un error del backend se muestra al usuario y reactiva el panel")
    void informaElErrorDelBackend() {
        VistaEstadisticas vista = mock(VistaEstadisticas.class);
        when(vista.getDesdeRecursos()).thenReturn(DESDE);
        when(vista.getHastaRecursos()).thenReturn(HASTA);
        when(servicio.categoriasReservadas(DESDE, HASTA))
                .thenThrow(new SistemaException("No hay conexión con el servidor."));
        ControladorEstadisticas controlador = new ControladorEstadisticas(servicio,
                new ModeloEstadisticas(), vista, new EjecutorDirecto());

        controlador.consultarRecursos();

        verify(vista).mostrarError("No hay conexión con el servidor.");
        verify(vista).habilitarRecursos(true);
    }

    @Test
    @DisplayName("Sin resultados no se exporta nada")
    void noExportaSinDatos() {
        VistaEstadisticas vista = mock(VistaEstadisticas.class);
        ControladorEstadisticas controlador = new ControladorEstadisticas(servicio,
                new ModeloEstadisticas(), vista, new EjecutorDirecto());

        controlador.imprimirRecursos();

        verify(vista).mostrarAdvertencia(contains("No hay datos"));
        verify(vista, never()).elegirDestinoPdf(anyString());
    }

    @Test
    @DisplayName("El reporte de recursos incluye la tabla y se guarda en el destino")
    void generaElReporteDeRecursos(@TempDir Path carpeta) throws Exception {
        ModeloEstadisticas modelo = new ModeloEstadisticas();
        modelo.establecerCategorias(categorias());
        Path destino = carpeta.resolve("estadisticas.pdf");
        VistaEstadisticas vista = mock(VistaEstadisticas.class);
        when(vista.elegirDestinoPdf(anyString())).thenReturn(destino);
        ControladorEstadisticas controlador = new ControladorEstadisticas(servicio, modelo,
                vista, new EjecutorDirecto());

        controlador.imprimirRecursos();

        assertTrue(Files.exists(destino));
        assertTrue(Files.size(destino) > 0);
        verify(vista).mostrarInformacion(contains(destino.toString()));
    }

    @Test
    @DisplayName("Si el usuario cancela el destino no se genera el archivo")
    void cancelarLaExportacionNoGeneraNada(@TempDir Path carpeta) throws Exception {
        ModeloEstadisticas modelo = new ModeloEstadisticas();
        modelo.establecerSemanas(semanas());
        VistaEstadisticas vista = mock(VistaEstadisticas.class);
        when(vista.elegirDestinoPdf(anyString())).thenReturn(null);
        ControladorEstadisticas controlador = new ControladorEstadisticas(servicio, modelo,
                vista, new EjecutorDirecto());

        controlador.imprimirActividades();

        try (var archivos = Files.list(carpeta)) {
            assertEquals(0, archivos.count());
        }
        verify(vista, never()).mostrarInformacion(anyString());
    }
}
