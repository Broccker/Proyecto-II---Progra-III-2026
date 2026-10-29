package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistema.common.protocolo.CargaNuevaReserva;
import com.sistema.common.protocolo.Notificacion;
import com.sistema.common.protocolo.TipoNotificacion;
import com.sistema.frontend.model.Alerta;
import com.sistema.frontend.model.ModeloAlertas;
import com.sistema.frontend.proxy.ExcepcionRed;
import com.sistema.frontend.view.PanelAlertas;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ControladorAlertasTest {

    private static final HiloInterfaz DIRECTO = Runnable::run;

    private ModeloAlertas modelo;
    private PanelAlertas vista;
    private HiloManual hilo;

    @BeforeEach
    void preparar() {
        modelo = new ModeloAlertas();
        vista = mock(PanelAlertas.class);
        hilo = new HiloManual();
    }

    private ControladorAlertas controlador(HiloInterfaz despachador) {
        ControladorAlertas controlador = new ControladorAlertas(modelo, vista, despachador);
        clearInvocations(vista);
        return controlador;
    }

    private static Notificacion notificacion(String reservaId, String encargado,
                                             String actividad) {
        return new Notificacion(TipoNotificacion.NUEVA_RESERVA,
                new CargaNuevaReserva(reservaId, encargado, actividad,
                        LocalDate.of(2026, 11, 2), LocalTime.of(9, 0),
                        LocalTime.of(11, 0), List.of()));
    }

    @Test
    @DisplayName("Una reserva nueva se agrega al modelo y refresca la tabla")
    void agregaYRefresca() {
        ControladorAlertas controlador = controlador(DIRECTO);

        controlador.alRecibir(notificacion("RES-000001", "Ana Pérez", "Reunión"));

        assertEquals(1, modelo.total());
        assertEquals("Ana Pérez - Reunión",
                modelo.getAlertas().get(0).textoActividad());
        verify(vista).actualizar();
    }

    @Test
    @DisplayName("Una ráfaga de alertas se vuelca en un único refresco")
    void agrupaLaRafaga() {
        ControladorAlertas controlador = controlador(hilo);

        for (int indice = 0; indice < 12; indice++) {
            controlador.alRecibir(notificacion("RES-0000" + indice, "Ana", "Actividad " + indice));
        }

        assertEquals(0, modelo.total());
        assertEquals(1, hilo.pendientes());
        verify(vista, never()).actualizar();

        hilo.correrTodas();

        assertEquals(12, modelo.total());
        verify(vista, times(1)).actualizar();
    }

    @Test
    @DisplayName("Ignora una notificación nula o de un tipo no soportado")
    void ignoraNotificacionesInvalidas() {
        ControladorAlertas controlador = controlador(DIRECTO);

        controlador.alRecibir(null);

        assertEquals(0, modelo.total());
        verify(vista, never()).actualizar();
    }

    @Test
    @DisplayName("Ver abre el detalle de la alerta de la fila")
    void verAbreElDetalle() {
        ControladorAlertas controlador = controlador(DIRECTO);
        controlador.alRecibir(notificacion("RES-000001", "Ana Pérez", "Reunión"));
        Alerta esperada = modelo.getAlertas().get(0);

        controlador.ver(0);

        verify(vista).mostrarDetalle(esperada);
    }

    @Test
    @DisplayName("Ver con una fila inexistente no abre nada")
    void verFilaInexistente() {
        ControladorAlertas controlador = controlador(DIRECTO);

        controlador.ver(3);
        controlador.ver(-1);

        verify(vista, never()).mostrarDetalle(any());
    }

    @Test
    @DisplayName("Descartar confirmado quita la alerta y refresca")
    void descartaLaAlerta() {
        ControladorAlertas controlador = controlador(DIRECTO);
        controlador.alRecibir(notificacion("RES-000001", "Ana Pérez", "Reunión"));
        when(vista.confirmarDescarte(any())).thenReturn(true);

        controlador.descartar(0);

        assertTrue(modelo.estaVacio());
        verify(vista, times(2)).actualizar();
    }

    @Test
    @DisplayName("Si el usuario cancela, la alerta permanece")
    void cancelarConservaLaAlerta() {
        ControladorAlertas controlador = controlador(DIRECTO);
        controlador.alRecibir(notificacion("RES-000001", "Ana Pérez", "Reunión"));
        when(vista.confirmarDescarte(any())).thenReturn(false);
        clearInvocations(vista);

        controlador.descartar(0);

        assertEquals(1, modelo.total());
        verify(vista, never()).actualizar();
    }

    @Test
    @DisplayName("La pérdida de conexión se informa una sola vez")
    void avisaUnaSolaVez() {
        ControladorAlertas controlador = controlador(DIRECTO);
        ExcepcionRed corte = new ExcepcionRed(ExcepcionRed.Motivo.CONEXION_PERDIDA,
                "Se perdió la conexión con el backend.");

        controlador.alPerderConexion(corte);
        controlador.alPerderConexion(corte);

        verify(vista, times(1)).mostrarDesconexion(contains("conexión"));
    }

    @Test
    @DisplayName("Al reconectar se restablece el panel y se vuelve a avisar si cae")
    void restableceElAvisoTrasReconectar() {
        ControladorAlertas controlador = controlador(DIRECTO);
        ExcepcionRed corte = new ExcepcionRed(ExcepcionRed.Motivo.CONEXION_PERDIDA,
                "Se perdió la conexión con el backend.");

        controlador.alPerderConexion(corte);
        controlador.conexionRestablecida();
        controlador.alPerderConexion(corte);

        verify(vista).mostrarConexion();
        verify(vista, times(2)).mostrarDesconexion(contains("conexión"));
    }

    @Test
    @DisplayName("Vaciar descarta las alertas de la sesión")
    void vaciarDescartaTodo() {
        ControladorAlertas controlador = controlador(DIRECTO);
        controlador.alRecibir(notificacion("RES-000001", "Ana Pérez", "Reunión"));
        clearInvocations(vista);

        controlador.vaciar();

        assertTrue(modelo.estaVacio());
        verify(vista).actualizar();
    }

    @Test
    @DisplayName("Al construir se pinta el estado inicial del panel")
    void pintaElEstadoInicial() {
        new ControladorAlertas(modelo, vista, DIRECTO);

        verify(vista).actualizar();
    }

    /** Despachador que retiene las tareas hasta que la prueba las ejecuta. */
    private static final class HiloManual implements HiloInterfaz {

        private final Deque<Runnable> tareas = new ArrayDeque<>();

        @Override
        public void ejecutar(Runnable tarea) {
            tareas.add(tarea);
        }

        private void correrTodas() {
            Runnable tarea;
            while ((tarea = tareas.poll()) != null) {
                tarea.run();
            }
        }

        private int pendientes() {
            return tareas.size();
        }
    }
}
