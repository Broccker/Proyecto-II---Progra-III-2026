package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistema.common.dto.MatrizCalendarizacion;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloCalendarizacion;
import com.sistema.frontend.view.VistaCalendarizacion;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ControladorCalendarizacionTest {

    private Service servicio;
    private ModeloCalendarizacion modelo;
    private VistaCalendarizacion vista;
    private ControladorCalendarizacion controlador;

    @BeforeEach
    void preparar() {
        servicio = mock(Service.class);
        modelo = new ModeloCalendarizacion();
        vista = new VistaCalendarizacion(modelo);
        controlador = new ControladorCalendarizacion(servicio, modelo, vista,
                new EjecutorDirecto());
        when(servicio.listarCategorias()).thenReturn(List.of(DatosControladores.categoria()));
        when(servicio.construirMatrizCalendarizacion(any(), eq("CAT-000001")))
                .thenReturn(DatosControladores.matrizCalendarizacion());
    }

    @Test
    @DisplayName("Cargar trae las categorías y la matriz del día actual")
    void cargaCategoriasYMatriz() {
        controlador.cargar();

        assertEquals(1, modelo.getCategorias().size());
        assertNotNull(modelo.getMatriz());
        assertEquals(2, vista.getTabla().getRowCount());
        assertEquals(2, vista.getTabla().getColumnCount());
        assertEquals("09:00", vista.getTabla().getValueAt(0, 0));
    }

    @Test
    @DisplayName("Consultar usa la categoría y la fecha elegidas")
    void consultaLaCategoriaElegida() {
        controlador.cargar();
        vista.establecerFecha(DatosControladores.FECHA);

        controlador.consultar();

        verify(servicio).construirMatrizCalendarizacion(DatosControladores.FECHA,
                "CAT-000001");
    }

    @Test
    @DisplayName("Sin categorías no construye ninguna matriz")
    void sinCategoriasNoConsulta() {
        when(servicio.listarCategorias()).thenReturn(List.of());

        controlador.cargar();

        verify(servicio, never()).construirMatrizCalendarizacion(any(), anyString());
    }

    @Test
    @DisplayName("Sin categoría seleccionada no consulta la matriz")
    void sinCategoriaSeleccionadaNoConsulta() {
        controlador.consultar();

        verify(servicio, never()).construirMatrizCalendarizacion(any(), anyString());
    }
}
