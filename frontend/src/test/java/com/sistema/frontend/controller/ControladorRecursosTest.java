package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloRecursos;
import com.sistema.frontend.view.VistaRecursos;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ControladorRecursosTest {

    private Service servicio;
    private ModeloRecursos modelo;
    private VistaRecursos vista;
    private ControladorRecursos controlador;

    @BeforeEach
    void preparar() {
        servicio = mock(Service.class);
        modelo = new ModeloRecursos();
        vista = new VistaRecursos(modelo);
        controlador = new ControladorRecursos(servicio, modelo, vista,
                new EjecutorDirecto());
        when(servicio.listarCategorias()).thenReturn(
                List.of(DatosControladores.categoria(), DatosControladores.otraCategoria()));
        when(servicio.filtrarRecursos(nullable(String.class), anyString()))
                .thenReturn(List.of(DatosControladores.recurso()));
    }

    @Test
    @DisplayName("Cargar trae categorías, recursos y arma la tabla")
    void cargaCategoriasYRecursos() {
        controlador.cargar();

        assertEquals(2, modelo.getCategorias().size());
        assertEquals(1, modelo.getRecursos().size());
        assertEquals(1, vista.getTabla().getRowCount());
        assertEquals("1001", vista.getTabla().getValueAt(0, 0));
        assertEquals("Proyectores", vista.getTabla().getValueAt(0, 1));
        verify(servicio).filtrarRecursos(null, "");
    }

    @Test
    @DisplayName("Cambiar el filtro consulta solo esa categoría")
    void cambiaElFiltroDeCategoria() {
        controlador.cargar();

        vista.establecerFiltroCategoria("CAT-000001");

        assertEquals("CAT-000001", modelo.getCategoriaFiltroId());
        verify(servicio).filtrarRecursos("CAT-000001", "");
    }

    @Test
    @DisplayName("Crear un recurso envía identificador, categoría y descripción")
    void creaUnRecurso() {
        controlador.cargar();
        controlador.limpiar();
        vista.establecerFormulario("1002", "CAT-000001", "Proyector nuevo");

        controlador.guardar();

        verify(servicio).altaRecurso("1002", "CAT-000001", "Proyector nuevo");
        assertNull(modelo.getSeleccionado());
    }

    @Test
    @DisplayName("Modificar envía el identificador original y el nuevo")
    void modificaElRecurso() {
        controlador.cargar();
        modelo.seleccionar(DatosControladores.recurso());
        vista.establecerFormulario("1001", "CAT-000001", "Proyector Epson 2");

        controlador.guardar();

        verify(servicio).modificarRecurso("1001", "1001", "CAT-000001",
                "Proyector Epson 2");
    }

    @Test
    @DisplayName("No guarda si falta la descripción")
    void exigeLaDescripcion() {
        controlador.cargar();
        vista.establecerFormulario("1002", "CAT-000001", "");

        controlador.guardar();

        verify(servicio, never()).altaRecurso(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Eliminar exige un recurso seleccionado")
    void eliminarSinSeleccionNoConsulta() {
        controlador.eliminar();

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Eliminar el recurso seleccionado invoca al backend")
    void eliminaElSeleccionado() {
        controlador.cargar();
        modelo.seleccionar(DatosControladores.recurso());

        controlador.eliminar();

        verify(servicio).eliminarRecurso("1001");
    }

    @Test
    @DisplayName("Seleccionar una fila vuelca el recurso en el formulario")
    void seleccionarFilaActualizaElFormulario() {
        controlador.cargar();

        vista.getTabla().setRowSelectionInterval(0, 0);

        assertEquals("1001", modelo.getSeleccionado().getId());
        assertEquals("1001", vista.getIdentificador());
        assertEquals("CAT-000001", vista.getCategoriaId());
        assertEquals("Proyector Epson", vista.getDescripcion());
        verify(servicio, never()).filtrarRecursos(eq("CAT-000002"), anyString());
    }
}
