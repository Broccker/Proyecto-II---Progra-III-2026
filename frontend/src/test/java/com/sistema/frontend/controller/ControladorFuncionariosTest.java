package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistema.common.excepciones.AccesoDatosException;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloFuncionarios;
import com.sistema.frontend.view.VistaFuncionarios;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ControladorFuncionariosTest {

    private Service servicio;
    private ModeloFuncionarios modelo;
    private VistaFuncionarios vista;
    private ControladorFuncionarios controlador;

    @BeforeEach
    void preparar() {
        servicio = mock(Service.class);
        modelo = new ModeloFuncionarios();
        vista = new VistaFuncionarios(modelo);
        controlador = new ControladorFuncionarios(servicio, modelo, vista,
                new EjecutorDirecto());
        when(servicio.buscarFuncionarios(""))
                .thenReturn(List.of(DatosControladores.funcionario(),
                        DatosControladores.otroFuncionario()));
    }

    @Test
    @DisplayName("Cargar vuelca el listado en el modelo y en la tabla")
    void cargaElListado() {
        controlador.cargar();

        assertEquals(2, modelo.getFuncionarios().size());
        assertEquals(2, vista.getTabla().getRowCount());
        assertEquals("222", vista.getTabla().getValueAt(0, 0));
        verify(servicio).buscarFuncionarios("");
    }

    @Test
    @DisplayName("La búsqueda envía el criterio escrito")
    void buscaConElCriterio() {
        when(servicio.buscarFuncionarios("Ana")).thenReturn(
                List.of(DatosControladores.funcionario()));
        vista.establecerCriterio("Ana");

        controlador.buscar();

        assertEquals("Ana", modelo.getCriterio());
        assertEquals(1, modelo.getFuncionarios().size());
        verify(servicio).buscarFuncionarios("Ana");
    }

    @Test
    @DisplayName("Guardar sin selección crea un funcionario")
    void creaUnFuncionario() {
        vista.establecerFormulario("444", "Pedro Pérez", "77776666");

        controlador.guardar();

        verify(servicio).altaFuncionario("444", "Pedro Pérez", "77776666");
        assertNull(modelo.getSeleccionado());
    }

    @Test
    @DisplayName("Guardar con selección modifica el funcionario")
    void modificaElFuncionarioSeleccionado() {
        modelo.seleccionar(DatosControladores.funcionario());
        vista.establecerFormulario("222", "Ana María", "88881111");

        controlador.guardar();

        verify(servicio).modificarFuncionario("222", "Ana María", "88881111");
    }

    @Test
    @DisplayName("No guarda si faltan datos obligatorios")
    void exigeLosDatos() {
        vista.establecerFormulario("", "", "");

        controlador.guardar();

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Eliminar exige una fila seleccionada")
    void eliminarSinSeleccionNoConsulta() {
        controlador.eliminar();

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Eliminar el seleccionado invoca al backend")
    void eliminaElSeleccionado() {
        modelo.seleccionar(DatosControladores.funcionario());

        controlador.eliminar();

        verify(servicio).eliminarFuncionario("222");
        assertNull(modelo.getSeleccionado());
    }

    @Test
    @DisplayName("Seleccionar una fila actualiza el modelo y el formulario")
    void seleccionarFilaActualizaElModelo() {
        controlador.cargar();

        vista.getTabla().setRowSelectionInterval(1, 1);

        assertEquals("333", modelo.getSeleccionado().getId());
        assertEquals("333", vista.getId());
    }

    @Test
    @DisplayName("Limpiar quita la selección y el formulario")
    void limpiarLimpiaElFormulario() {
        modelo.seleccionar(DatosControladores.funcionario());
        vista.mostrarSeleccion();

        controlador.limpiar();

        assertNull(modelo.getSeleccionado());
        assertEquals("", vista.getId());
    }

    @Test
    @DisplayName("Un fallo de red no se propaga al hilo de la interfaz")
    void falloDeRedSeInforma() {
        when(servicio.buscarFuncionarios("")).thenThrow(
                new AccesoDatosException("El servidor no responde."));

        controlador.cargar();

        assertTrue(modelo.getFuncionarios().isEmpty());
    }
}
