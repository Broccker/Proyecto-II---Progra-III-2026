package com.sistema.backend.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Recurso;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoriaRecursoDAOIT extends PruebaBDBase {

    private CategoriaRecursoDAO dao() {
        return new CategoriaRecursoDAO(fuente());
    }

    @Test
    @DisplayName("El primer identificador generado es CAT-000001")
    void siguienteIdConTablaVacia() {
        assertEquals("CAT-000001", dao().siguienteId());
    }

    @Test
    @DisplayName("El siguiente identificador continúa la numeración")
    void siguienteIdContinuaLaNumeracion() {
        crearCategoria("CAT-000001", "Proyectores");
        crearCategoria("CAT-000007", "Salas");

        assertEquals("CAT-000008", dao().siguienteId());
    }

    @Test
    @DisplayName("Inserta, lista y busca categorías")
    void insertaListaYBusca() {
        dao().insertar(new CategoriaRecurso("CAT-000001", "Proyectores"));
        dao().insertar(new CategoriaRecurso("CAT-000002", "Salas de reuniones"));

        assertEquals(2, dao().listarTodas().size());
        assertEquals(1, dao().buscar("proyector").size());
        assertEquals(1, dao().buscar("CAT-000002").size());
        assertEquals(2, dao().buscar("").size());
        assertTrue(dao().buscarPorId("CAT-000001").isPresent());
        assertTrue(dao().buscarPorDescripcion("Proyectores").isPresent());
    }

    @Test
    @DisplayName("La descripción es única sin distinguir mayúsculas")
    void rechazaDescripcionDuplicada() {
        dao().insertar(new CategoriaRecurso("CAT-000001", "Proyectores"));

        assertThrows(ConflictoException.class,
                () -> dao().insertar(new CategoriaRecurso("CAT-000002", "proyectores")));
        assertTrue(dao().existeDescripcion("PROYECTORES"));
        assertTrue(dao().existeDescripcion("proyectores", "CAT-000002"));
        assertEquals(1, contar("categorias"));
    }

    @Test
    @DisplayName("Actualiza la descripción de una categoría")
    void actualizaDescripcion() {
        crearCategoria("CAT-000001", "Proyectores");

        dao().actualizar("CAT-000001", new CategoriaRecurso("CAT-000001", "Equipos de proyección"));

        assertEquals("Equipos de proyección",
                dao().buscarPorId("CAT-000001").orElseThrow().getDescripcion());
    }

    @Test
    @DisplayName("Renombrar el id se propaga a los recursos (ON UPDATE CASCADE)")
    void renombrarIdSePropagaARecursos() {
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        crearRecurso("1001", categoria, "Proyector Epson");

        dao().actualizar("CAT-000001", new CategoriaRecurso("CAT-000009", "Proyectores"));

        Recurso recurso = new RecursoDAO(fuente()).buscarPorId("1001").orElseThrow();
        assertEquals("CAT-000009", recurso.getCategoria().getId());
        assertTrue(dao().buscarPorId("CAT-000001").isEmpty());
    }

    @Test
    @DisplayName("No permite eliminar una categoría con recursos")
    void noEliminaCategoriaConRecursos() {
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        crearRecurso("1001", categoria, "Proyector Epson");

        assertEquals(1, dao().contarRecursos("CAT-000001"));
        assertThrows(ConflictoException.class, () -> dao().eliminar("CAT-000001"));
        assertTrue(dao().buscarPorId("CAT-000001").isPresent());
    }

    @Test
    @DisplayName("Elimina una categoría sin recursos")
    void eliminaCategoriaVacia() {
        crearCategoria("CAT-000001", "Proyectores");

        dao().eliminar("CAT-000001");

        assertEquals(List.of(), dao().listarTodas());
        assertEquals(0, contar("categorias"));
    }

    @Test
    @DisplayName("Fallar al modificar una categoría inexistente")
    void actualizaInexistente() {
        assertThrows(NoEncontradoException.class,
                () -> dao().actualizar("CAT-999999",
                        new CategoriaRecurso("CAT-999999", "Fantasma")));
        assertThrows(NoEncontradoException.class, () -> dao().eliminar("CAT-999999"));
    }
}
