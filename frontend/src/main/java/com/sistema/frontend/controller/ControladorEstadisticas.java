package com.sistema.frontend.controller;

import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloEstadisticas;
import com.sistema.frontend.util.Reportes;
import com.sistema.frontend.view.VistaEstadisticas;
import com.sistema.frontend.view.componentes.SelectorFecha;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.jfree.chart.JFreeChart;

/**
 * Controlador de la vista de estadísticas.
 *
 * <p>Cada panel se consulta por separado con su propio rango de fechas. Las
 * consultas agregadas las resuelve el backend en SQL; el controlador solo
 * valida el rango, lanza la petición fuera del hilo de eventos y vuelca el
 * resultado en el modelo.</p>
 */
public final class ControladorEstadisticas {

    private static final String[] ENCABEZADOS_RECURSOS = {"Categoría", "Reservas"};
    private static final String[] ENCABEZADOS_ACTIVIDADES =
            {"Semana (lunes)", "Actividades"};

    private final Service servicio;
    private final ModeloEstadisticas modelo;
    private final VistaEstadisticas vista;
    private final EjecutorTareas ejecutor;

    public ControladorEstadisticas(Service servicio, ModeloEstadisticas modelo,
                                   VistaEstadisticas vista, EjecutorTareas ejecutor) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio es obligatorio.");
        this.modelo = Objects.requireNonNull(modelo, "El modelo es obligatorio.");
        this.vista = Objects.requireNonNull(vista, "La vista es obligatoria.");
        this.ejecutor = Objects.requireNonNull(ejecutor, "El ejecutor es obligatorio.");

        vista.alCalcularRecursos(this::consultarRecursos);
        vista.alCalcularActividades(this::consultarActividades);
        vista.alImprimirRecursos(this::imprimirRecursos);
        vista.alImprimirActividades(this::imprimirActividades);
    }

    /** Carga ambos paneles con sus rangos iniciales. */
    public void cargar() {
        consultarRecursos();
        consultarActividades();
    }

    /** Consulta las reservas por categoría del rango del panel de recursos. */
    void consultarRecursos() {
        LocalDate desde;
        LocalDate hasta;
        try {
            desde = vista.getDesdeRecursos();
            hasta = vista.getHastaRecursos();
            validarRango(desde, hasta);
        } catch (IllegalArgumentException error) {
            vista.mostrarAdvertencia(error.getMessage());
            return;
        }

        vista.habilitarRecursos(false);
        AtomicReference<List<ConteoCategoria>> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(servicio.categoriasReservadas(desde, hasta)),
                () -> {
                    vista.habilitarRecursos(true);
                    modelo.establecerCategorias(resultado.get());
                    vista.actualizar();
                },
                error -> {
                    vista.habilitarRecursos(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    /** Consulta las actividades por semana del rango del panel de actividades. */
    void consultarActividades() {
        LocalDate desde;
        LocalDate hasta;
        try {
            desde = vista.getDesdeActividades();
            hasta = vista.getHastaActividades();
            validarRango(desde, hasta);
        } catch (IllegalArgumentException error) {
            vista.mostrarAdvertencia(error.getMessage());
            return;
        }

        vista.habilitarActividades(false);
        AtomicReference<List<ConteoSemana>> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(servicio.actividadesPorSemana(desde, hasta)),
                () -> {
                    vista.habilitarActividades(true);
                    modelo.establecerSemanas(resultado.get());
                    vista.actualizar();
                },
                error -> {
                    vista.habilitarActividades(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    void imprimirRecursos() {
        List<ConteoCategoria> categorias = modelo.getCategorias();
        List<String[]> filas = new ArrayList<>();
        for (ConteoCategoria conteo : categorias) {
            filas.add(new String[]{conteo.categoria().getDescripcion(),
                    String.valueOf(conteo.cantidad())});
        }
        imprimir("estadisticas-recursos.pdf", "Categorías reservadas",
                ENCABEZADOS_RECURSOS, filas, vista.getGraficoRecursos());
    }

    void imprimirActividades() {
        List<ConteoSemana> semanas = modelo.getSemanas();
        List<String[]> filas = new ArrayList<>();
        for (ConteoSemana conteo : semanas) {
            filas.add(new String[]{conteo.lunes().format(SelectorFecha.FORMATO),
                    String.valueOf(conteo.cantidad())});
        }
        imprimir("estadisticas-actividades.pdf", "Actividades por semana",
                ENCABEZADOS_ACTIVIDADES, filas, vista.getGraficoActividades());
    }

    private void imprimir(String nombreSugerido, String titulo, String[] encabezados,
                          List<String[]> filas, JFreeChart grafico) {
        if (filas.isEmpty()) {
            vista.mostrarAdvertencia("No hay datos para exportar.");
            return;
        }
        Path destino = vista.elegirDestinoPdf(nombreSugerido);
        if (destino == null) {
            return;
        }
        BufferedImage imagen = grafico == null ? null : grafico.createBufferedImage(780, 260);
        vista.habilitar(false);
        ejecutor.ejecutar(
                () -> Reportes.generarConGrafico(titulo, encabezados, filas, imagen, destino),
                () -> {
                    vista.habilitar(true);
                    vista.mostrarInformacion("Reporte generado en " + destino + ".");
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha desde no puede ser posterior a la fecha hasta.");
        }
    }
}
