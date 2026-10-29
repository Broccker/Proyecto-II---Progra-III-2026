package com.sistema.frontend.controller;

import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloCategorias;
import com.sistema.frontend.util.Reportes;
import com.sistema.frontend.util.Validaciones;
import com.sistema.frontend.view.VistaCategorias;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controlador del módulo de categorías de recursos.
 *
 * <p>El identificador de una categoría nueva lo asigna el backend; al
 * modificar se puede cambiar y el servidor actualiza las referencias.</p>
 */
public final class ControladorCategorias {

    private static final String[] ENCABEZADOS = {"Identificador", "Descripción"};

    private final Service servicio;
    private final ModeloCategorias modelo;
    private final VistaCategorias vista;
    private final EjecutorTareas ejecutor;

    public ControladorCategorias(Service servicio, ModeloCategorias modelo,
                                 VistaCategorias vista, EjecutorTareas ejecutor) {
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

    void buscar() {
        String criterio = vista.getCriterio();
        modelo.establecerCriterio(criterio);
        vista.habilitar(false);
        AtomicReference<List<CategoriaRecurso>> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(servicio.buscarCategorias(criterio)),
                () -> {
                    vista.habilitar(true);
                    modelo.establecerCategorias(resultado.get());
                    vista.actualizar();
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    void guardar() {
        String descripcion;
        String nuevoId;
        try {
            descripcion = Validaciones.texto(vista.getDescripcion(),
                    "La descripción de la categoría",
                    Validaciones.MAX_DESCRIPCION_CATEGORIA);
            nuevoId = Validaciones.opcional(vista.getIdentificador(),
                    "El identificador de la categoría", Validaciones.MAX_ID);
        } catch (IllegalArgumentException e) {
            vista.mostrarAdvertencia(e.getMessage());
            return;
        }

        CategoriaRecurso seleccionada = modelo.getSeleccionada();
        vista.habilitar(false);
        AtomicReference<CategoriaRecurso> creada = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> {
                    if (seleccionada == null) {
                        creada.set(servicio.altaCategoria(descripcion));
                    } else {
                        servicio.modificarCategoria(seleccionada.getId(), nuevoId, descripcion);
                    }
                },
                () -> {
                    vista.habilitar(true);
                    String mensaje = seleccionada == null
                            ? "La categoría " + creada.get().getId() + " fue creada."
                            : "La categoría " + seleccionada.getId() + " fue actualizada.";
                    vista.mostrarInformacion(mensaje);
                    modelo.limpiarSeleccion();
                    vista.limpiarFormulario();
                    buscar();
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    void eliminar() {
        CategoriaRecurso seleccionada = modelo.getSeleccionada();
        if (seleccionada == null) {
            vista.mostrarAdvertencia("Seleccione una categoría de la tabla.");
            return;
        }
        if (!vista.confirmarEliminacion(seleccionada.getId())) {
            return;
        }
        vista.habilitar(false);
        ejecutor.ejecutar(
                () -> servicio.eliminarCategoria(seleccionada.getId()),
                () -> {
                    vista.habilitar(true);
                    vista.mostrarInformacion("La categoría " + seleccionada.getId()
                            + " fue eliminada.");
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
        List<CategoriaRecurso> categorias = modelo.getCategorias();
        if (fila < 0 || fila >= categorias.size()) {
            return;
        }
        modelo.seleccionar(categorias.get(fila));
        vista.mostrarSeleccion();
    }

    private void imprimir() {
        List<CategoriaRecurso> categorias = modelo.getCategorias();
        if (categorias.isEmpty()) {
            vista.mostrarAdvertencia("No hay categorías para exportar.");
            return;
        }
        List<String[]> filas = new ArrayList<>();
        for (CategoriaRecurso categoria : categorias) {
            filas.add(new String[]{categoria.getId(), categoria.getDescripcion()});
        }
        Path destino = vista.elegirDestinoPdf("categorias.pdf");
        if (destino == null) {
            return;
        }
        vista.habilitar(false);
        ejecutor.ejecutar(
                () -> Reportes.generar("Categorías de recursos", ENCABEZADOS, filas, destino),
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
