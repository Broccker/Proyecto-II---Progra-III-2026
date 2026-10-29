package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistema.common.excepciones.AccesoDatosException;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloActividades;
import com.sistema.frontend.view.VistaActividades;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ControladorActividadesTest {

    private Service servicio;
    private ModeloActividades modelo;
    private VistaActividades vista;
    private ControladorActividades controlador;

    @BeforeEach
    void preparar() {
        servicio = mock(Service.class);
        modelo = new ModeloActividades();
        vista = new VistaActividades(modelo);
        controlador = new ControladorActividades(servicio, modelo, vista,
                new EjecutorDirecto());
        when(servicio.construirMatrizActividades(any()))
                .thenReturn(DatosControladores.matrizActividades());
    }

    @Test
    @DisplayName("Cargar pinta la matriz semanal de 24 horas por 7 días")
    void cargaLaMatrizSemanal() {
        vista.establecerFecha(DatosControladores.FECHA);

        controlador.cargar();

        assertNotNull(modelo.getMatriz());
        verify(servicio).construirMatrizActividades(DatosControladores.FECHA);
        assertEquals(2, vista.getTabla().getRowCount());
        assertEquals(3, vista.getTabla().getColumnCount());
        assertEquals("Hora", vista.getTabla().getColumnName(0));
        assertEquals("Lunes", vista.getTabla().getColumnName(1));
        assertEquals("09:00", vista.getTabla().getValueAt(0, 0));
    }

    @Test
    @DisplayName("Un fallo del backend deja la vista sin matriz y no propaga la excepción")
    void falloDelBackendSeInforma() {
        when(servicio.construirMatrizActividades(any()))
                .thenThrow(new AccesoDatosException("El servidor no responde."));

        controlador.cargar();

        assertNull(modelo.getMatriz());
        assertEquals(0, vista.getTabla().getRowCount());
    }
}
