package com.sistema.backend.logic.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.modelo.CategoriaRecurso;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapeoCategoriasTest {

    private static final CategoriaRecurso SALAS = new CategoriaRecurso("CAT-000001", "Salas");
    private static final CategoriaRecurso PROYECTORES =
            new CategoriaRecurso("CAT-000002", "Proyectores");

    @Test
    @DisplayName("Resuelve nombres sin distinguir mayúsculas y descarta repetidos")
    void resuelveSinDistinguirMayusculas() {
        MapeoCategorias.ResultadoMapeo resultado = MapeoCategorias.mapear(
                List.of("salas", "SALAS", " Proyectores "),
                List.of(SALAS, PROYECTORES));

        assertEquals(List.of(SALAS, PROYECTORES), resultado.encontradas());
        assertTrue(resultado.noEncontradas().isEmpty());
    }

    @Test
    @DisplayName("Informa los nombres que no existen en el sistema")
    void informaNombresDesconocidos() {
        MapeoCategorias.ResultadoMapeo resultado = MapeoCategorias.mapear(
                List.of("Salas", "Drones", "drones"),
                List.of(SALAS));

        assertEquals(List.of(SALAS), resultado.encontradas());
        assertEquals(List.of("Drones"), resultado.noEncontradas());
    }

    @Test
    @DisplayName("Tolera listas y nombres nulos o vacíos")
    void toleraValoresVacios() {
        MapeoCategorias.ResultadoMapeo vacio =
                MapeoCategorias.mapear(null, List.of(SALAS));
        assertTrue(vacio.encontradas().isEmpty());
        assertTrue(vacio.noEncontradas().isEmpty());

        MapeoCategorias.ResultadoMapeo conNulos = MapeoCategorias.mapear(
                java.util.Arrays.asList(null, "  ", "Salas"), List.of(SALAS));
        assertEquals(List.of(SALAS), conNulos.encontradas());
        assertTrue(conNulos.noEncontradas().isEmpty());
    }
}
