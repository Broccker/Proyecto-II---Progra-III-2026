package com.sistema.frontend.controller;

import com.sistema.common.dto.CeldaCalendario;
import com.sistema.common.dto.MatrizCalendarizacion;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloCalendarizacion;
import com.sistema.frontend.util.Reportes;
import com.sistema.frontend.view.VistaCalendarizacion;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controlador de la calendarización diaria por categoría.
 *
 * <p>Primero trae las categorías; después construye la matriz de ocupación de
 * la categoría y la fecha elegidas. La matriz la calcula el backend a partir
 * de las reservas vigentes.</p>
 */
public final class ControladorCalendarizacion {

    private final Service servicio;
    private final ModeloCalendarizacion modelo;
    private final VistaCalendarizacion vista;
    private final EjecutorTareas ejecutor;

    public ControladorCalendarizacion(Service servicio, ModeloCalendarizacion modelo,
                                      VistaCalendarizacion vista, EjecutorTareas ejecutor) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio es obligatorio.");
        this.modelo = Objects.requireNonNull(modelo, "El modelo es obligatorio.");
        this.vista = Objects.requireNonNull(vista, "La vista es obligatoria.");
        this.ejecutor = Objects.requireNonNull(ejecutor, "El ejecutor es obligatorio.");

        vista.alCargar(this::consultar);
        vista.alImprimir(this::imprimir);
    }

    /** Carga las categorías y, si hay alguna, la matriz del día actual. */
    public void cargar() {
        vista.habilitar(false);
        AtomicReference<List<CategoriaRecurso>> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(servicio.listarCategorias()),
                () -> {
                    vista.habilitar(true);
                    modelo.establecerCategorias(resultado.get());
                    vista.actualizar();
                    if (vista.getCategoriaId() != null) {
                        consultar();
                    }
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    /** Construye la matriz de la categoría y fecha seleccionadas. */
    void consultar() {
        String categoriaId = vista.getCategoriaId();
        if (categoriaId == null) {
            vista.mostrarAdvertencia("Seleccione una categoría.");
            return;
        }
        LocalDate fecha;
        try {
            fecha = vista.getFecha();
        } catch (IllegalArgumentException e) {
            vista.mostrarAdvertencia(e.getMessage());
            return;
        }

        modelo.establecerCategoria(categoriaId);
        vista.habilitar(false);
        AtomicReference<MatrizCalendarizacion> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(
                        servicio.construirMatrizCalendarizacion(fecha, categoriaId)),
                () -> {
                    vista.habilitar(true);
                    modelo.establecerMatriz(resultado.get());
                    vista.actualizar();
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    private void imprimir() {
        MatrizCalendarizacion matriz = modelo.getMatriz();
        if (matriz == null) {
            vista.mostrarAdvertencia("Cargue la calendarización antes de exportarla.");
            return;
        }
        List<String> encabezados = new ArrayList<>();
        encabezados.add("Hora");
        for (Recurso recurso : matriz.recursos()) {
            encabezados.add(recurso.getDescripcion());
        }
        List<String[]> filas = new ArrayList<>();
        for (int indiceHora = 0; indiceHora < matriz.horas().size(); indiceHora++) {
            String[] fila = new String[encabezados.size()];
            fila[0] = matriz.horas().get(indiceHora).toString();
            List<CeldaCalendario> celdas = matriz.celdas().get(indiceHora);
            for (int indiceRecurso = 0; indiceRecurso < celdas.size(); indiceRecurso++) {
                fila[indiceRecurso + 1] = celdas.get(indiceRecurso).texto();
            }
            filas.add(fila);
        }
        Path destino = vista.elegirDestinoPdf("calendarizacion.pdf");
        if (destino == null) {
            return;
        }
        String titulo = "Calendarización de " + matriz.categoria().getDescripcion()
                + " — " + matriz.fecha().format(
                        com.sistema.frontend.view.componentes.SelectorFecha.FORMATO);
        vista.habilitar(false);
        ejecutor.ejecutar(
                () -> Reportes.generar(titulo, encabezados.toArray(new String[0]),
                        filas, destino),
                () -> {
                    vista.habilitar(true);
                    vista.mostrarInformacion("Reporte generado en " + destino + ".");
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }
}
