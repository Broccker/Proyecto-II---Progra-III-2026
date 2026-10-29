package com.sistema.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.CategoriaRecurso;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceImplCategoriasTest extends PruebaServicioBase {

    @Test
    @DisplayName("Crea una categoría con el siguiente identificador disponible")
    void creaCategoriaConSiguienteId() {
        when(categoriaDAO.existeDescripcion("Salas")).thenReturn(false);
        when(categoriaDAO.siguienteId()).thenReturn("CAT-000004");

        CategoriaRecurso creada = servicio.altaCategoria("  Salas  ");

        assertEquals("CAT-000004", creada.getId());
        assertEquals("Salas", creada.getDescripcion());
        verify(categoriaDAO).insertar(creada);
    }

    @Test
    @DisplayName("Rechaza una descripción duplicada o vacía")
    void rechazaDescripcionDuplicadaOVacia() {
        when(categoriaDAO.existeDescripcion("Salas")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> servicio.altaCategoria("Salas"));
        assertThrows(ValidacionException.class, () -> servicio.altaCategoria("  "));
        verify(categoriaDAO, never()).insertar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Modifica el identificador y la descripción de una categoría")
    void modificaCategoriaConNuevoId() {
        CategoriaRecurso existente = categoria("CAT-000001", "Salas");
        when(categoriaDAO.buscarPorId("CAT-000001")).thenReturn(Optional.of(existente));
        when(categoriaDAO.existeId("CAT-000009")).thenReturn(false);
        when(categoriaDAO.existeDescripcion("Salas de reunión", "CAT-000001")).thenReturn(false);

        servicio.modificarCategoria("CAT-000001", "CAT-000009", "Salas de reunión");

        ArgumentCaptor<CategoriaRecurso> capturada = ArgumentCaptor.forClass(CategoriaRecurso.class);
        verify(categoriaDAO).actualizar(org.mockito.ArgumentMatchers.eq("CAT-000001"),
                capturada.capture());
        assertEquals("CAT-000009", capturada.getValue().getId());
        assertEquals("Salas de reunión", capturada.getValue().getDescripcion());
    }

    @Test
    @DisplayName("Conserva el identificador cuando no se indica uno nuevo")
    void conservaIdSiNoSeIndica() {
        when(categoriaDAO.buscarPorId("CAT-000001"))
                .thenReturn(Optional.of(categoria("CAT-000001", "Salas")));
        when(categoriaDAO.existeDescripcion("Salas nuevas", "CAT-000001")).thenReturn(false);

        servicio.modificarCategoria("CAT-000001", "  ", "Salas nuevas");

        ArgumentCaptor<CategoriaRecurso> capturada = ArgumentCaptor.forClass(CategoriaRecurso.class);
        verify(categoriaDAO).actualizar(org.mockito.ArgumentMatchers.eq("CAT-000001"),
                capturada.capture());
        assertEquals("CAT-000001", capturada.getValue().getId());
    }

    @Test
    @DisplayName("Rechaza modificar una categoría inexistente o con un id ya usado")
    void rechazaModificacionInvalida() {
        when(categoriaDAO.buscarPorId("CAT-999999")).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class,
                () -> servicio.modificarCategoria("CAT-999999", null, "Salas"));

        when(categoriaDAO.buscarPorId("CAT-000001"))
                .thenReturn(Optional.of(categoria("CAT-000001", "Salas")));
        when(categoriaDAO.existeId("CAT-000002")).thenReturn(true);
        assertThrows(ConflictoException.class,
                () -> servicio.modificarCategoria("CAT-000001", "CAT-000002", "Salas"));
        verify(categoriaDAO, never()).actualizar(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("No elimina una categoría con recursos asociados")
    void rechazaEliminarCategoriaConRecursos() {
        when(categoriaDAO.buscarPorId("CAT-000001"))
                .thenReturn(Optional.of(categoria("CAT-000001", "Salas")));
        when(categoriaDAO.contarRecursos("CAT-000001")).thenReturn(2L);

        assertThrows(ConflictoException.class, () -> servicio.eliminarCategoria("CAT-000001"));
        verify(categoriaDAO, never()).eliminar("CAT-000001");
    }

    @Test
    @DisplayName("Elimina una categoría sin recursos")
    void eliminaCategoriaSinRecursos() {
        when(categoriaDAO.buscarPorId("CAT-000001"))
                .thenReturn(Optional.of(categoria("CAT-000001", "Salas")));
        when(categoriaDAO.contarRecursos("CAT-000001")).thenReturn(0L);

        servicio.eliminarCategoria("CAT-000001");

        verify(categoriaDAO).eliminar("CAT-000001");
    }

    @Test
    @DisplayName("Lista, busca y comprueba descripciones delegando en el DAO")
    void consultaCategorias() {
        List<CategoriaRecurso> categorias = List.of(categoria("CAT-000001", "Salas"));
        when(categoriaDAO.listarTodas()).thenReturn(categorias);
        when(categoriaDAO.buscar("Sal")).thenReturn(categorias);
        when(categoriaDAO.existeDescripcion("Salas")).thenReturn(true);

        assertEquals(categorias, servicio.listarCategorias());
        assertEquals(categorias, servicio.buscarCategorias("Sal"));
        assertTrue(servicio.existeDescripcionCategoria("Salas"));
        assertFalse(servicio.existeDescripcionCategoria("  "));
    }

    @Test
    @DisplayName("Una descripción vacía no consulta la base")
    void descripcionVaciaNoConsulta() {
        assertFalse(servicio.existeDescripcionCategoria(null));
        assertFalse(servicio.existeDescripcionCategoria(""));

        verifyNoInteractions(categoriaDAO);
    }
}
