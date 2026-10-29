package com.sistema.frontend.controller;

import com.sistema.common.dto.CeldaCalendario;
import com.sistema.common.dto.MatrizActividades;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloActividades;
import com.sistema.frontend.util.Reportes;
import com.sistema.frontend.view.VistaActividades;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controlador de la vista semanal de actividades.
 *
 * <p>La matriz de 24 horas por 7 días la construye el backend a partir de las
 * reservas activas de la semana que contiene la fecha de referencia.</p>
 */
public final class ControladorActividades {

    private final Service servicio;
    private final ModeloActividades modelo;
    private final VistaActividades vista;
    private final EjecutorTareas ejecutor;

    public ControladorActividades(Service servicio, ModeloActividades modelo,
                                  VistaActividades vista, EjecutorTareas ejecutor) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio es obligatorio.");
        this.modelo = Objects.requireNonNull(modelo, "El modelo es obligatorio.");
        this.vista = Objects.requireNonNull(vista, "La vista es obligatoria.");
        this.ejecutor = Objects.requireNonNull(ejecutor, "El ejecutor es obligatorio.");

        vista.alCargar(this::consultar);
        vista.alImprimir(this::imprimir);
    }

    /** Carga la semana que contiene la fecha mostrada. */
    public void cargar() {
        consultar();
    }

    /** Construye y muestra la matriz de la semana seleccionada. */
    void consultar() {
        LocalDate fecha;
        try {
            fecha = vista.getFecha();
        } catch (IllegalArgumentException e) {
            vista.mostrarAdvertencia(e.getMessage());
            return;
        }

        vista.habilitar(false);
        AtomicReference<MatrizActividades> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(servicio.construirMatrizActividades(fecha)),
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
        MatrizActividades matriz = modelo.getMatriz();
        if (matriz == null) {
            vista.mostrarAdvertencia("Cargue las actividades antes de exportarlas.");
            return;
        }
        List<String> encabezados = new ArrayList<>();
        encabezados.add("Hora");
        encabezados.addAll(matriz.dias());
        List<String[]> filas = new ArrayList<>();
        for (int indiceHora = 0; indiceHora < matriz.horas().size(); indiceHora++) {
            String[] fila = new String[encabezados.size()];
            fila[0] = matriz.horas().get(indiceHora).toString();
            List<CeldaCalendario> celdas = matriz.celdas().get(indiceHora);
            for (int indiceDia = 0; indiceDia < celdas.size(); indiceDia++) {
                fila[indiceDia + 1] = celdas.get(indiceDia).texto();
            }
            filas.add(fila);
        }
        Path destino = vista.elegirDestinoPdf("actividades.pdf");
        if (destino == null) {
            return;
        }
        String titulo = "Actividades de la semana del "
                + matriz.lunes().format(
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
