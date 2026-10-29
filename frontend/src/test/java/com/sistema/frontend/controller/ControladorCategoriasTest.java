package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloCategorias;
import com.sistema.frontend.view.VistaCategorias;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ControladorCategoriasTest {

    private Service servicio;
    private ModeloCategorias modelo;
    private VistaCategorias vista;
    private ControladorCategorias controlador;

    @BeforeEach
    void preparar() {
        servicio = mock(Service.class);
        modelo = new ModeloCategorias();
        vista = new VistaCategorias(modelo);
        controlador = new ControladorCategorias(servicio, modelo, vista,
                new EjecutorDirecto());
        when(servicio.buscarCategorias("")).thenReturn(
                List.of(DatosControladores.categoria(), DatosControladores.otraCategoria()));
    }

    @Test
    @DisplayName("Cargar muestra las categorías en la tabla")
    void cargaElListado() {
        controlador.cargar();

        assertEquals(2, modelo.getCategorias().size());
        assertEquals(2, vista.getTabla().getRowCount());
        assertEquals("CAT-000001", vista.getTabla().getValueAt(0, 0));
    }

    @Test
    @DisplayName("Crear una categoría usa el identificador que asigna el backend")
    void creaUnaCategoria() {
        CategoriaRecurso creada = new CategoriaRecurso("CAT-000003", "Impresoras");
        when(servicio.altaCategoria("Impresoras")).thenReturn(creada);
        controlador.cargar();
        controlador.limpiar();
        vista.establecerFormulario("", "Impresoras");

        controlador.guardar();

        verify(servicio).altaCategoria("Impresoras");
        assertNull(modelo.getSeleccionada());
    }

    @Test
    @DisplayName("Modificar envía el identificador actual y el nuevo")
    void modificaLaCategoria() {
        controlador.cargar();
        modelo.seleccionar(DatosControladores.categoria());
        vista.establecerFormulario("CAT-000001", "Proyectores de techo");

        controlador.guardar();

        verify(servicio).modificarCategoria("CAT-000001", "CAT-000001",
                "Proyectores de techo");
    }

    @Test
    @DisplayName("No guarda una descripción vacía")
    void exigeLaDescripcion() {
        controlador.limpiar();
        vista.establecerFormulario("", "   ");

        controlador.guardar();

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Eliminar exige una categoría seleccionada")
    void eliminarSinSeleccionNoConsulta() {
        controlador.eliminar();

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Eliminar la categoría seleccionada invoca al backend")
    void eliminaLaSeleccionada() {
        controlador.cargar();
        modelo.seleccionar(DatosControladores.categoria());

        controlador.eliminar();

        verify(servicio).eliminarCategoria("CAT-000001");
    }

    @Test
    @DisplayName("Seleccionar una fila actualiza el modelo y permite editar el identificador")
    void seleccionarFilaActualizaElModelo() {
        controlador.cargar();

        vista.getTabla().setRowSelectionInterval(1, 1);

        assertEquals("CAT-000002", modelo.getSeleccionada().getId());
        assertEquals("CAT-000002", vista.getIdentificador());
    }
}
