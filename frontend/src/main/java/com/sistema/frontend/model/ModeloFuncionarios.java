package com.sistema.frontend.model;

import com.sistema.common.modelo.Funcionario;
import java.util.List;
import java.util.Objects;

/**
 * Estado del módulo de funcionarios: listado visible, criterio de búsqueda y
 * fila seleccionada.
 */
public final class ModeloFuncionarios {

    private List<Funcionario> funcionarios = List.of();
    private Funcionario seleccionado;
    private String criterio = "";

    public List<Funcionario> getFuncionarios() {
        return funcionarios;
    }

    public void establecerFuncionarios(List<Funcionario> funcionarios) {
        Objects.requireNonNull(funcionarios, "La lista de funcionarios es obligatoria.");
        this.funcionarios = List.copyOf(funcionarios);
        if (seleccionado != null && !this.funcionarios.contains(seleccionado)) {
            seleccionado = null;
        }
    }

    public Funcionario getSeleccionado() {
        return seleccionado;
    }

    public void seleccionar(Funcionario funcionario) {
        this.seleccionado = funcionario;
    }

    public void limpiarSeleccion() {
        seleccionado = null;
    }

    public String getCriterio() {
        return criterio;
    }

    public void establecerCriterio(String criterio) {
        this.criterio = criterio == null ? "" : criterio.trim();
    }
}
