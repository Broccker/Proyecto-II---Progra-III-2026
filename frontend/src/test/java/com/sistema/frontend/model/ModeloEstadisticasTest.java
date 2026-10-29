package com.sistema.frontend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import com.sistema.common.modelo.CategoriaRecurso;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModeloEstadisticasTest {

    private ModeloEstadisticas modelo;

    @BeforeEach
    void preparar() {
        modelo = new ModeloEstadisticas();
    }

    private static ConteoCategoria categoria(String id, String descripcion, long cantidad) {
        return new ConteoCategoria(new CategoriaRecurso(id, descripcion), cantidad);
    }

    @Test
    @DisplayName("Arranca sin resultados")
    void arrancaVacio() {
        assertTrue(modelo.getCategorias().isEmpty());
        assertTrue(modelo.getSemanas().isEmpty());
    }

    @Test
    @DisplayName("Guarda los conteos por categoría")
    void guardaCategorias() {
        List<ConteoCategoria> categorias = List.of(
                categoria("CAT-000001", "Proyectores", 5),
                categoria("CAT-000002", "Salas", 2));

        modelo.establecerCategorias(categorias);

        assertEquals(2, modelo.getCategorias().size());
        assertEquals(5, modelo.getCategorias().get(0).cantidad());
    }

    @Test
    @DisplayName("Guarda los conteos por semana")
    void guardaSemanas() {
        modelo.establecerSemanas(List.of(
                new ConteoSemana(LocalDate.of(2026, 11, 2), 3)));

        assertEquals(1, modelo.getSemanas().size());
        assertEquals(LocalDate.of(2026, 11, 2), modelo.getSemanas().get(0).lunes());
    }

    @Test
    @DisplayName("Una lista nula se interpreta como resultado vacío")
    void toleraListasNulas() {
        modelo.establecerCategorias(null);
        modelo.establecerSemanas(null);

        assertTrue(modelo.getCategorias().isEmpty());
        assertTrue(modelo.getSemanas().isEmpty());
    }

    @Test
    @DisplayName("El modelo conserva una copia inmutable del resultado")
    void copiaElResultado() {
        List<ConteoCategoria> original = new ArrayList<>();
        original.add(categoria("CAT-000001", "Proyectores", 5));

        modelo.establecerCategorias(original);
        original.clear();

        assertEquals(1, modelo.getCategorias().size());
        assertEquals("Proyectores", modelo.getCategorias().get(0).categoria().getDescripcion());
    }
}
