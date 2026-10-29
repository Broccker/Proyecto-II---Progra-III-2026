package com.sistema.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Recurso;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceImplRecursosTest extends PruebaServicioBase {

    private static final CategoriaRecurso PROYECTORES =
            categoria("CAT-000001", "Proyectores");

    @Test
    @DisplayName("Da de alta un recurso en una categoría existente")
    void daDeAltaRecurso() {
        when(categoriaDAO.buscarPorId("CAT-000001")).thenReturn(Optional.of(PROYECTORES));
        when(recursoDAO.existeId("1001")).thenReturn(false);
        when(recursoDAO.existeDescripcionEnCategoria("CAT-000001", "Proyector Epson", null))
                .thenReturn(false);

        servicio.altaRecurso(" 1001 ", "CAT-000001", " Proyector Epson ");

        ArgumentCaptor<Recurso> capturado = ArgumentCaptor.forClass(Recurso.class);
        verify(recursoDAO).insertar(capturado.capture());
        assertEquals("1001", capturado.getValue().getId());
        assertEquals("Proyector Epson", capturado.getValue().getDescripcion());
        assertEquals("CAT-000001", capturado.getValue().getCategoria().getId());
    }

    @Test
    @DisplayName("Rechaza un recurso con categoría inexistente")
    void rechazaCategoriaInexistente() {
        when(categoriaDAO.buscarPorId("CAT-999999")).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> servicio.altaRecurso("1001", "CAT-999999", "Proyector"));
        verify(recursoDAO, never()).insertar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Rechaza identificadores y descripciones duplicadas")
    void rechazaDuplicados() {
        when(categoriaDAO.buscarPorId("CAT-000001")).thenReturn(Optional.of(PROYECTORES));
        when(recursoDAO.existeId("1001")).thenReturn(true);
        assertThrows(ConflictoException.class,
                () -> servicio.altaRecurso("1001", "CAT-000001", "Proyector"));

        when(recursoDAO.existeId("1002")).thenReturn(false);
        when(recursoDAO.existeDescripcionEnCategoria("CAT-000001", "Proyector", null))
                .thenReturn(true);
        assertThrows(ConflictoException.class,
                () -> servicio.altaRecurso("1002", "CAT-000001", "Proyector"));
        verify(recursoDAO, never()).insertar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Modifica un recurso, incluido su identificador")
    void modificaRecurso() {
        when(categoriaDAO.buscarPorId("CAT-000001")).thenReturn(Optional.of(PROYECTORES));
        when(recursoDAO.buscarPorId("1001"))
                .thenReturn(Optional.of(recurso("1001", PROYECTORES, "Proyector Epson")));
        when(recursoDAO.existeId("1009")).thenReturn(false);
        when(recursoDAO.existeDescripcionEnCategoria("CAT-000001", "Proyector Epson X", "1001"))
                .thenReturn(false);

        servicio.modificarRecurso("1001", "1009", "CAT-000001", "Proyector Epson X");

        ArgumentCaptor<Recurso> capturado = ArgumentCaptor.forClass(Recurso.class);
        verify(recursoDAO).actualizar(org.mockito.ArgumentMatchers.eq("1001"),
                capturado.capture());
        assertEquals("1009", capturado.getValue().getId());
        assertEquals("Proyector Epson X", capturado.getValue().getDescripcion());
    }

    @Test
    @DisplayName("Rechaza modificar un recurso inexistente")
    void rechazaRecursoInexistente() {
        when(categoriaDAO.buscarPorId("CAT-000001")).thenReturn(Optional.of(PROYECTORES));
        when(recursoDAO.buscarPorId("9999")).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> servicio.modificarRecurso("9999", null, "CAT-000001", "Proyector"));
    }

    @Test
    @DisplayName("No elimina un recurso incluido en reservas")
    void rechazaEliminarRecursoReservado() {
        when(recursoDAO.buscarPorId("1001"))
                .thenReturn(Optional.of(recurso("1001", PROYECTORES, "Proyector")));
        when(reservaDAO.existeRecursoReservado("1001")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> servicio.eliminarRecurso("1001"));
        verify(recursoDAO, never()).eliminar("1001");
    }

    @Test
    @DisplayName("Elimina un recurso sin reservas")
    void eliminaRecursoLibre() {
        when(recursoDAO.buscarPorId("1001"))
                .thenReturn(Optional.of(recurso("1001", PROYECTORES, "Proyector")));
        when(reservaDAO.existeRecursoReservado("1001")).thenReturn(false);

        servicio.eliminarRecurso("1001");

        verify(recursoDAO).eliminar("1001");
    }

    @Test
    @DisplayName("Lista, busca y filtra recursos delegando en el DAO")
    void consultaRecursos() {
        List<Recurso> recursos = List.of(recurso("1001", PROYECTORES, "Proyector"));
        when(recursoDAO.listarTodos()).thenReturn(recursos);
        when(recursoDAO.buscar("Proyector")).thenReturn(recursos);
        when(recursoDAO.filtrar("CAT-000001", "Proyector")).thenReturn(recursos);

        assertEquals(recursos, servicio.listarRecursos());
        assertEquals(recursos, servicio.buscarRecursos("Proyector"));
        assertEquals(recursos, servicio.filtrarRecursos("CAT-000001", "Proyector"));
    }
}
