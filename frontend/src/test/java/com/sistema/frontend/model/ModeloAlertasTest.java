package com.sistema.frontend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.protocolo.CargaNuevaReserva;
import com.sistema.common.protocolo.Notificacion;
import com.sistema.common.protocolo.TipoNotificacion;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModeloAlertasTest {

    private ModeloAlertas modelo;

    @BeforeEach
    void preparar() {
        modelo = new ModeloAlertas();
    }

    private static Alerta alerta(String reservaId) {
        return new Alerta(reservaId, "Ana Pérez", "Reunión de equipo",
                LocalDate.of(2026, 11, 2), LocalTime.of(9, 0), LocalTime.of(11, 0),
                List.of());
    }

    @Test
    @DisplayName("Arranca vacío")
    void arrancaVacio() {
        assertTrue(modelo.estaVacio());
        assertEquals(0, modelo.total());
        assertTrue(modelo.getAlertas().isEmpty());
    }

    @Test
    @DisplayName("Agrega alertas en orden de llegada")
    void agregaEnOrden() {
        assertTrue(modelo.agregar(alerta("RES-000002")));
        assertTrue(modelo.agregar(alerta("RES-000001")));

        assertEquals(2, modelo.total());
        assertEquals("RES-000002", modelo.getAlertas().get(0).reservaId());
        assertEquals("RES-000001", modelo.getAlertas().get(1).reservaId());
    }

    @Test
    @DisplayName("No duplica una reserva ya registrada")
    void noDuplica() {
        assertTrue(modelo.agregar(alerta("RES-000001")));
        assertFalse(modelo.agregar(alerta("RES-000001")));

        assertEquals(1, modelo.total());
        assertTrue(modelo.contieneReserva("RES-000001"));
    }

    @Test
    @DisplayName("Agrega desde una notificación de reserva nueva")
    void agregaDesdeNotificacion() {
        Notificacion notificacion = new Notificacion(TipoNotificacion.NUEVA_RESERVA,
                new CargaNuevaReserva("RES-000003", "Luis Rojas", "Charla",
                        LocalDate.of(2026, 11, 4), LocalTime.of(8, 0),
                        LocalTime.of(9, 0), List.of()));

        assertTrue(modelo.agregar(notificacion));
        assertEquals("Luis Rojas - Charla", modelo.getAlertas().get(0).textoActividad());
    }

    @Test
    @DisplayName("Eliminar quita solo la alerta indicada")
    void eliminaUna() {
        Alerta primera = alerta("RES-000001");
        Alerta segunda = alerta("RES-000002");
        modelo.agregar(primera);
        modelo.agregar(segunda);

        assertTrue(modelo.eliminar(primera));

        assertEquals(1, modelo.total());
        assertSame(segunda, modelo.getAlertas().get(0));
        assertFalse(modelo.eliminar(primera));
    }

    @Test
    @DisplayName("Vaciar descarta todas las alertas")
    void vacia() {
        modelo.agregar(alerta("RES-000001"));
        modelo.agregar(alerta("RES-000002"));

        modelo.vaciar();

        assertTrue(modelo.estaVacio());
    }
}
