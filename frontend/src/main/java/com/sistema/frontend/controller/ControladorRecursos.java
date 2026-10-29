package com.sistema.frontend.controller;

import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloRecursos;
import com.sistema.frontend.util.Reportes;
import com.sistema.frontend.view.VistaRecursos;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controlador del módulo de recursos.
 *
 * <p>En cada consulta se traen las categorías (para los filtros y el
 * formulario) y los recursos que cumplen el filtro vigente.</p>
 */
public final class ControladorRecursos {

    private static final String[] ENCABEZADOS =
            {"Identificador", "Categoría", "Descripción"};

    private final Service servicio;
    private final ModeloRecursos modelo;
    private final VistaRecursos vista;
    private final EjecutorTareas ejecutor;

    public ControladorRecursos(Service servicio, ModeloRecursos modelo,
                               VistaRecursos vista, EjecutorTareas ejecutor) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio es obligatorio.");
        this.modelo = Objects.requireNonNull(modelo, "El modelo es obligatorio.");
        this.vista = Objects.requireNonNull(vista, "La vista es obligatoria.");
        this.ejecutor = Objects.requireNonNull(ejecutor, "El ejecutor es obligatorio.");

        vista.alBuscar(this::buscar);
        vista.alGuardar(this::guardar);
        vista.alEliminar(this::eliminar);
        vista.alLimpiar(this::limpiar);
        vista.alImprimir(this::imprimir);
        vista.alCambiarFiltro(this::cambiarFiltro);
        vista.alSeleccionarFila(this::seleccionar);
    }

    /** Carga inicial: categorías y recursos. */
    public void cargar() {
        recargar();
    }

    void buscar() {
        modelo.establecerCriterio(vista.getCriterio());
        recargar();
    }

    void cambiarFiltro() {
        modelo.establecerCategoriaFiltro(vista.getFiltroCategoriaId());
        recargar();
    }

    private void recargar() {
        String categoriaId = modelo.getCategoriaFiltroId();
        String criterio = modelo.getCriterio();
        vista.habilitar(false);
        AtomicReference<Carga> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(new Carga(servicio.listarCategorias(),
                        servicio.filtrarRecursos(categoriaId, criterio))),
                () -> {
                    vista.habilitar(true);
                    Carga carga = resultado.get();
                    modelo.establecerCategorias(carga.categorias());
                    modelo.establecerRecursos(carga.recursos());
                    vista.actualizar();
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    void guardar() {
        String id = vista.getIdentificador();
        String categoriaId = vista.getCategoriaId();
        String descripcion = vista.getDescripcion();
        if (id.isBlank()) {
            vista.mostrarAdvertencia("Ingrese el identificador del recurso.");
            return;
        }
        if (categoriaId == null || categoriaId.isBlank()) {
            vista.mostrarAdvertencia("Seleccione la categoría del recurso.");
            return;
        }
        if (descripcion.isBlank()) {
            vista.mostrarAdvertencia("Ingrese la descripción del recurso.");
            return;
        }

        Recurso seleccionado = modelo.getSeleccionado();
        vista.habilitar(false);
        ejecutor.ejecutar(
                () -> {
                    if (seleccionado == null) {
                        servicio.altaRecurso(id, categoriaId, descripcion);
                    } else {
                        servicio.modificarRecurso(seleccionado.getId(), id, categoriaId,
                                descripcion);
                    }
                },
                () -> {
                    vista.habilitar(true);
                    vista.mostrarInformacion(seleccionado == null
                            ? "El recurso " + id + " fue creado."
                            : "El recurso " + id + " fue actualizado.");
                    modelo.limpiarSeleccion();
                    vista.limpiarFormulario();
                    recargar();
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    void eliminar() {
        Recurso seleccionado = modelo.getSeleccionado();
        if (seleccionado == null) {
            vista.mostrarAdvertencia("Seleccione un recurso de la tabla.");
            return;
        }
        if (!vista.confirmarEliminacion(seleccionado.getId())) {
            return;
        }
        vista.habilitar(false);
        ejecutor.ejecutar(
                () -> servicio.eliminarRecurso(seleccionado.getId()),
                () -> {
                    vista.habilitar(true);
                    vista.mostrarInformacion("El recurso " + seleccionado.getId()
                            + " fue eliminado.");
                    modelo.limpiarSeleccion();
                    vista.limpiarFormulario();
                    recargar();
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
        List<Recurso> recursos = modelo.getRecursos();
        if (fila < 0 || fila >= recursos.size()) {
            return;
        }
        modelo.seleccionar(recursos.get(fila));
        vista.mostrarSeleccion();
    }

    private void imprimir() {
        List<Recurso> recursos = modelo.getRecursos();
        if (recursos.isEmpty()) {
            vista.mostrarAdvertencia("No hay recursos para exportar.");
            return;
        }
        List<String[]> filas = new ArrayList<>();
        for (Recurso recurso : recursos) {
            String categoria = recurso.getCategoria() == null
                    ? "" : recurso.getCategoria().getDescripcion();
            filas.add(new String[]{recurso.getId(), categoria, recurso.getDescripcion()});
        }
        Path destino = vista.elegirDestinoPdf("recursos.pdf");
        if (destino == null) {
            return;
        }
        vista.habilitar(false);
        ejecutor.ejecutar(
                () -> Reportes.generar("Recursos", ENCABEZADOS, filas, destino),
                () -> {
                    vista.habilitar(true);
                    vista.mostrarInformacion("Reporte generado en " + destino + ".");
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    /** Resultado de una consulta: catálogo de categorías y recursos filtrados. */
    private record Carga(List<CategoriaRecurso> categorias, List<Recurso> recursos) {
    }
}
