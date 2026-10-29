package com.sistema.frontend.controller;

import com.sistema.common.modelo.Funcionario;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloFuncionarios;
import com.sistema.frontend.util.Reportes;
import com.sistema.frontend.util.Validaciones;
import com.sistema.frontend.view.VistaFuncionarios;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controlador del módulo de funcionarios.
 *
 * <p>Une la vista, el modelo del módulo y el backend. La vista expone las
 * intenciones del usuario; aquí se validan, se decide entre alta y
 * modificación y se actualiza el estado compartido.</p>
 */
public final class ControladorFuncionarios {

    private static final String[] ENCABEZADOS = {"Identificador", "Nombre", "Teléfono"};

    private final Service servicio;
    private final ModeloFuncionarios modelo;
    private final VistaFuncionarios vista;
    private final EjecutorTareas ejecutor;

    public ControladorFuncionarios(Service servicio, ModeloFuncionarios modelo,
                                   VistaFuncionarios vista, EjecutorTareas ejecutor) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio es obligatorio.");
        this.modelo = Objects.requireNonNull(modelo, "El modelo es obligatorio.");
        this.vista = Objects.requireNonNull(vista, "La vista es obligatoria.");
        this.ejecutor = Objects.requireNonNull(ejecutor, "El ejecutor es obligatorio.");

        vista.alBuscar(this::buscar);
        vista.alGuardar(this::guardar);
        vista.alEliminar(this::eliminar);
        vista.alLimpiar(this::limpiar);
        vista.alImprimir(this::imprimir);
        vista.alSeleccionarFila(this::seleccionar);
    }

    /** Carga inicial del listado. */
    public void cargar() {
        buscar();
    }

    /** Consulta el backend con el criterio escrito en la vista. */
    void buscar() {
        String criterio = vista.getCriterio();
        modelo.establecerCriterio(criterio);
        vista.habilitar(false);
        AtomicReference<List<Funcionario>> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(servicio.buscarFuncionarios(criterio)),
                () -> {
                    vista.habilitar(true);
                    modelo.establecerFuncionarios(resultado.get());
                    vista.actualizar();
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    /** Crea un funcionario nuevo o guarda los cambios del seleccionado. */
    void guardar() {
        String id;
        String nombre;
        String telefono;
        try {
            id = Validaciones.identificador(vista.getId(),
                    "El identificador del funcionario");
            nombre = Validaciones.texto(vista.getNombre(), "El nombre",
                    Validaciones.MAX_NOMBRE);
            telefono = Validaciones.telefono(vista.getTelefono());
        } catch (IllegalArgumentException e) {
            vista.mostrarAdvertencia(e.getMessage());
            return;
        }

        Funcionario seleccionado = modelo.getSeleccionado();
        vista.habilitar(false);
        ejecutor.ejecutar(
                () -> {
                    if (seleccionado == null) {
                        servicio.altaFuncionario(id, nombre, telefono);
                    } else {
                        servicio.modificarFuncionario(seleccionado.getId(), nombre, telefono);
                    }
                },
                () -> {
                    vista.habilitar(true);
                    vista.mostrarInformacion(seleccionado == null
                            ? "El funcionario " + id + " fue creado."
                            : "El funcionario " + id + " fue actualizado.");
                    modelo.limpiarSeleccion();
                    vista.limpiarFormulario();
                    buscar();
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    /** Elimina el funcionario seleccionado, previa confirmación. */
    void eliminar() {
        Funcionario seleccionado = modelo.getSeleccionado();
        if (seleccionado == null) {
            vista.mostrarAdvertencia("Seleccione un funcionario de la tabla.");
            return;
        }
        if (!vista.confirmarEliminacion(seleccionado.getId())) {
            return;
        }
        vista.habilitar(false);
        ejecutor.ejecutar(
                () -> servicio.eliminarFuncionario(seleccionado.getId()),
                () -> {
                    vista.habilitar(true);
                    vista.mostrarInformacion("El funcionario " + seleccionado.getId()
                            + " fue eliminado.");
                    modelo.limpiarSeleccion();
                    vista.limpiarFormulario();
                    buscar();
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    void limpiar() {
        modelo.limpiarSeleccion();
        vista.limpiarFormulario();
    }

    private void seleccionar(int fila) {
        List<Funcionario> funcionarios = modelo.getFuncionarios();
        if (fila < 0 || fila >= funcionarios.size()) {
            return;
        }
        modelo.seleccionar(funcionarios.get(fila));
        vista.mostrarSeleccion();
    }

    private void imprimir() {
        List<Funcionario> funcionarios = modelo.getFuncionarios();
        if (funcionarios.isEmpty()) {
            vista.mostrarAdvertencia("No hay funcionarios para exportar.");
            return;
        }
        List<String[]> filas = new ArrayList<>();
        for (Funcionario funcionario : funcionarios) {
            filas.add(new String[]{funcionario.getId(), funcionario.getNombre(),
                    funcionario.getTelefono()});
        }
        Path destino = vista.elegirDestinoPdf("funcionarios.pdf");
        if (destino == null) {
            return;
        }
        vista.habilitar(false);
        ejecutor.ejecutar(
                () -> Reportes.generar("Funcionarios", ENCABEZADOS, filas, destino),
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
