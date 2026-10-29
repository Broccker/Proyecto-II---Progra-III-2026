package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistema.common.dto.DatosReservaSugerida;
import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloReservas;
import com.sistema.frontend.view.VistaMisReservas;
import com.sistema.frontend.view.VistaNuevaReserva;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ControladorReservasTest {

    private Service servicio;
    private ModeloReservas modelo;
    private VistaNuevaReserva vistaNueva;
    private VistaMisReservas vistaMisReservas;
    private ControladorReservas controlador;

    @BeforeEach
    void preparar() {
        servicio = mock(Service.class);
        modelo = new ModeloReservas();
        vistaNueva = new VistaNuevaReserva(modelo);
        vistaMisReservas = new VistaMisReservas(modelo);
        controlador = new ControladorReservas(servicio, modelo, vistaNueva,
                vistaMisReservas, DatosControladores.ID_FUNCIONARIO, new EjecutorDirecto());
        when(servicio.listarCategorias()).thenReturn(List.of(DatosControladores.categoria()));
        when(servicio.listarReservasDe(DatosControladores.ID_FUNCIONARIO))
                .thenReturn(List.of(DatosControladores.reserva()));
    }

    @Test
    @DisplayName("Cargar trae categorías y el historial del funcionario")
    void cargaCategoriasYReservas() {
        controlador.cargar();

        assertEquals(1, modelo.getCategorias().size());
        assertEquals(1, modelo.getReservas().size());
        assertEquals(1, vistaMisReservas.getTabla().getRowCount());
        assertEquals("RES-000001", vistaMisReservas.getTabla().getValueAt(0, 0));
        assertEquals(1, vistaNueva.getListaCategorias().getModel().getSize());
    }

    @Test
    @DisplayName("Reservar envía la actividad, fecha, horario y categorías")
    void creaLaReserva() {
        when(servicio.crearReserva(eq(DatosControladores.ID_FUNCIONARIO),
                eq(DatosControladores.ACTIVIDAD), eq(DatosControladores.FECHA),
                eq(DatosControladores.INICIO), eq(DatosControladores.FIN),
                eq(List.of(DatosControladores.ID_CATEGORIA))))
                .thenReturn(DatosControladores.reserva());
        controlador.cargar();
        vistaNueva.establecerActividad(DatosControladores.ACTIVIDAD);
        vistaNueva.establecerFecha(DatosControladores.FECHA);
        vistaNueva.establecerHorario(DatosControladores.INICIO, DatosControladores.FIN);
        vistaNueva.seleccionarCategorias(List.of(DatosControladores.ID_CATEGORIA));

        controlador.reservar();

        verify(servicio).crearReserva(DatosControladores.ID_FUNCIONARIO,
                DatosControladores.ACTIVIDAD, DatosControladores.FECHA,
                DatosControladores.INICIO, DatosControladores.FIN,
                List.of(DatosControladores.ID_CATEGORIA));
        assertEquals("", vistaNueva.getActividad());
    }

    @Test
    @DisplayName("No reserva sin actividad")
    void exigeLaActividad() {
        controlador.cargar();
        vistaNueva.seleccionarCategorias(List.of(DatosControladores.ID_CATEGORIA));

        controlador.reservar();

        verify(servicio, never()).crearReserva(anyString(), anyString(), any(), any(),
                any(), anyList());
    }

    @Test
    @DisplayName("No reserva sin categorías")
    void exigeCategorias() {
        controlador.cargar();
        vistaNueva.establecerActividad(DatosControladores.ACTIVIDAD);

        controlador.reservar();

        verify(servicio, never()).crearReserva(anyString(), anyString(), any(), any(),
                any(), anyList());
    }

    @Test
    @DisplayName("Un conflicto del backend conserva el formulario para corregirlo")
    void conflictoConservaElFormulario() {
        when(servicio.crearReserva(anyString(), anyString(), any(), any(), any(),
                anyList())).thenThrow(new ConflictoException("No hay recursos disponibles."));
        controlador.cargar();
        vistaNueva.establecerActividad(DatosControladores.ACTIVIDAD);
        vistaNueva.establecerFecha(DatosControladores.FECHA);
        vistaNueva.establecerHorario(DatosControladores.INICIO, DatosControladores.FIN);
        vistaNueva.seleccionarCategorias(List.of(DatosControladores.ID_CATEGORIA));

        controlador.reservar();

        assertEquals(DatosControladores.ACTIVIDAD, vistaNueva.getActividad());
        assertEquals(List.of(DatosControladores.ID_CATEGORIA),
                vistaNueva.getIdsCategoriasSeleccionadas());
    }

    @Test
    @DisplayName("Extraer con IA rellena el formulario con la propuesta")
    void extraeLosDatosConIa() {
        DatosReservaSugerida sugerida = new DatosReservaSugerida("Clase de prueba",
                DatosControladores.FECHA, DatosControladores.INICIO,
                DatosControladores.FIN, List.of(DatosControladores.categoria()),
                List.of("Impresoras"));
        when(servicio.extraerDatosReserva("Clase de prueba el lunes")).thenReturn(sugerida);
        controlador.cargar();
        vistaNueva.establecerFrase("Clase de prueba el lunes");

        controlador.extraer();

        assertEquals("Clase de prueba", vistaNueva.getActividad());
        assertEquals(DatosControladores.FECHA, vistaNueva.getFecha());
        assertEquals(DatosControladores.INICIO, vistaNueva.getHoraInicio());
        assertEquals(List.of(DatosControladores.ID_CATEGORIA),
                vistaNueva.getIdsCategoriasSeleccionadas());
    }

    @Test
    @DisplayName("No consulta la IA con una frase vacía")
    void exigeLaFrase() {
        controlador.extraer();

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Cancelar exige una reserva seleccionada")
    void cancelarSinSeleccionNoConsulta() {
        controlador.cancelar();

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Cancelar la reserva seleccionada invoca al backend")
    void cancelaLaSeleccionada() {
        controlador.cargar();
        modelo.seleccionar(DatosControladores.reserva());

        controlador.cancelar();

        verify(servicio).cancelarReserva(DatosControladores.ID_RESERVA,
                DatosControladores.ID_FUNCIONARIO);
        assertNull(modelo.getSeleccionada());
    }
}
