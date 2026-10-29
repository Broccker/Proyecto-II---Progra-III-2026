package com.sistema.backend.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecursoDAOIT extends PruebaBDBase {

    private RecursoDAO dao() {
        return new RecursoDAO(fuente());
    }

    @Test
    @DisplayName("Inserta y recupera un recurso con su categoría")
    void insertaYRecuperaConCategoria() {
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        dao().insertar(new Recurso("1001", categoria, "Proyector Epson EB-X51"));

        Recurso recurso = dao().buscarPorId("1001").orElseThrow();

        assertEquals("Proyector Epson EB-X51", recurso.getDescripcion());
        assertEquals("CAT-000001", recurso.getCategoria().getId());
        assertEquals("Proyectores", recurso.getCategoria().getDescripcion());
        assertTrue(dao().existeId("1001"));
        assertEquals(1, dao().listarTodos().size());
    }

    @Test
    @DisplayName("Filtra por categoría y por texto")
    void filtraPorCategoriaYTexto() {
        CategoriaRecurso proyectores = crearCategoria("CAT-000001", "Proyectores");
        CategoriaRecurso salas = crearCategoria("CAT-000002", "Salas de reuniones");
        crearRecurso("1001", proyectores, "Proyector Epson");
        crearRecurso("2001", salas, "Sala A");

        assertEquals(List.of("1001"), ids(dao().filtrar("CAT-000001", null)));
        assertEquals(List.of("2001"), ids(dao().filtrar(null, "Sala")));
        assertEquals(List.of("1001"), ids(dao().filtrar(null, "Proyector")));
        assertEquals(2, dao().filtrar(null, "").size());
        assertEquals(List.of(), dao().filtrar("CAT-000001", "Sala"));
        assertEquals(List.of("2001"), ids(dao().listarPorCategoria("CAT-000002")));
    }

    @Test
    @DisplayName("Busca por identificador, descripción o categoría")
    void buscaPorTexto() {
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        crearRecurso("1001", categoria, "Proyector Epson");

        assertEquals(1, dao().buscar("1001").size());
        assertEquals(1, dao().buscar("Epson").size());
        assertEquals(1, dao().buscar("Proyectores").size());
        assertEquals(0, dao().buscar("impresora").size());
    }

    @Test
    @DisplayName("Rechaza una categoría inexistente")
    void rechazaCategoriaInexistente() {
        CategoriaRecurso fantasma = new CategoriaRecurso("CAT-999999", "Fantasma");

        assertThrows(ConflictoException.class,
                () -> dao().insertar(new Recurso("1001", fantasma, "Proyector")));
        assertEquals(0, contar("recursos"));
    }

    @Test
    @DisplayName("La descripción es única dentro de la misma categoría")
    void rechazaDescripcionDuplicadaEnLaCategoria() {
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        crearRecurso("1001", categoria, "Proyector Epson");

        assertThrows(ConflictoException.class,
                () -> crearRecurso("1002", categoria, "Proyector Epson"));
        assertEquals(1, contar("recursos"));
    }

    @Test
    @DisplayName("Permite la misma descripción en categorías distintas")
    void permiteMismaDescripcionEnOtraCategoria() {
        CategoriaRecurso proyectores = crearCategoria("CAT-000001", "Proyectores");
        CategoriaRecurso salas = crearCategoria("CAT-000002", "Salas");
        crearRecurso("1001", proyectores, "Equipo estándar");
        crearRecurso("2001", salas, "Equipo estándar");

        assertEquals(2, contar("recursos"));
    }

    @Test
    @DisplayName("Actualiza descripción y categoría")
    void actualizaRecurso() {
        CategoriaRecurso proyectores = crearCategoria("CAT-000001", "Proyectores");
        CategoriaRecurso salas = crearCategoria("CAT-000002", "Salas");
        crearRecurso("1001", proyectores, "Proyector Epson");

        dao().actualizar("1001", new Recurso("1001", salas, "Sala pequeña"));

        Recurso actualizado = dao().buscarPorId("1001").orElseThrow();
        assertEquals("CAT-000002", actualizado.getCategoria().getId());
        assertEquals("Sala pequeña", actualizado.getDescripcion());
    }

    @Test
    @DisplayName("No permite eliminar un recurso usado en una reserva")
    void noEliminaRecursoConReservas() {
        Funcionario funcionario = crearFuncionario("222", "Ana Pérez", "88881111");
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        Recurso recurso = crearRecurso("1001", categoria, "Proyector Epson");
        insertarReserva("RES-000001", funcionario, LocalDate.of(2026, 3, 2),
                "09:00", "10:00", recurso);

        assertThrows(ConflictoException.class, () -> dao().eliminar("1001"));
        assertTrue(dao().buscarPorId("1001").isPresent());
    }

    @Test
    @DisplayName("Elimina un recurso libre y falla si no existe")
    void eliminaRecursoLibre() {
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        crearRecurso("1001", categoria, "Proyector Epson");

        dao().eliminar("1001");

        assertTrue(dao().buscarPorId("1001").isEmpty());
        assertThrows(NoEncontradoException.class, () -> dao().eliminar("1001"));
        assertThrows(NoEncontradoException.class,
                () -> dao().actualizar("9999",
                        new Recurso("9999", categoria, "Fantasma")));
    }

    private static List<String> ids(List<Recurso> recursos) {
        return recursos.stream().map(Recurso::getId).toList();
    }
}
