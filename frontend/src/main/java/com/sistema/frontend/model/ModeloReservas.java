package com.sistema.frontend.model;

import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Reserva;
import java.util.List;
import java.util.Objects;

/**
 * Estado del módulo de reservas: categorías disponibles, reservas del
 * funcionario y reserva seleccionada.
 */
public final class ModeloReservas {

    private List<Reserva> reservas = List.of();
    private List<CategoriaRecurso> categorias = List.of();
    private Reserva seleccionada;

    public List<Reserva> getReservas() {
        return reservas;
    }

    public void establecerReservas(List<Reserva> reservas) {
        Objects.requireNonNull(reservas, "La lista de reservas es obligatoria.");
        this.reservas = List.copyOf(reservas);
        if (seleccionada != null && !this.reservas.contains(seleccionada)) {
            seleccionada = null;
        }
    }

    public List<CategoriaRecurso> getCategorias() {
        return categorias;
    }

    public void establecerCategorias(List<CategoriaRecurso> categorias) {
        Objects.requireNonNull(categorias, "La lista de categorías es obligatoria.");
        this.categorias = List.copyOf(categorias);
    }

    public Reserva getSeleccionada() {
        return seleccionada;
    }

    public void seleccionar(Reserva reserva) {
        this.seleccionada = reserva;
    }

    public void limpiarSeleccion() {
        seleccionada = null;
    }
}
