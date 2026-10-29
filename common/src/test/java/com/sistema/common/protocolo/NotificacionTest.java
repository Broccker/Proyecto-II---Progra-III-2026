package com.sistema.common.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.EstadoReserva;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificacionTest extends PruebaProtocoloBase {

    private static final Funcionario ANA = new Funcionario("222", "Ana Pérez", "88881111");
    private static final CategoriaRecurso PROYECTORES =
            new CategoriaRecurso("CAT-000001", "Proyectores");
    private static final Recurso PROYECTOR =
            new Recurso("1001", PROYECTORES, "Proyector Epson");
    private static final Reserva RESERVA = new Reserva("RES-000001", ANA,
            "Reunión de equipo", LocalDate.of(2026, 3, 2), LocalTime.of(9, 0),
            LocalTime.of(11, 0), List.of(PROYECTOR), EstadoReserva.ACTIVA);

    @Test
    @DisplayName("Serializa la notificación de una reserva nueva con todos sus datos")
    void serializaNuevaReserva() {
        Notificacion original = Notificacion.nuevaReserva(RESERVA);

        Notificacion recibida = idaYVuelta(original);
        CargaNuevaReserva carga = recibida.comoNuevaReserva();

        assertEquals(TipoNotificacion.NUEVA_RESERVA, recibida.tipo());
        assertInstanceOf(CargaNuevaReserva.class, recibida.carga());
        assertEquals("RES-000001", carga.reservaId());
        assertEquals("Ana Pérez", carga.encargado());
        assertEquals("Reunión de equipo", carga.actividad());
        assertEquals(LocalDate.of(2026, 3, 2), carga.fecha());
        assertEquals(LocalTime.of(9, 0), carga.horaInicio());
        assertEquals(LocalTime.of(11, 0), carga.horaFin());
        assertEquals(1, carga.recursos().size());
        assertEquals("1001", carga.recursos().getFirst().getId());
        assertEquals("Proyector Epson", carga.recursos().getFirst().getDescripcion());
    }

    @Test
    @DisplayName("Valida los datos obligatorios de la carga")
    void validaCarga() {
        assertThrows(ValidacionException.class, () -> new CargaNuevaReserva(
                " ", "Ana", "Reunión", LocalDate.now(), LocalTime.of(9, 0),
                LocalTime.of(10, 0), List.of()));
        assertThrows(ValidacionException.class, () -> new CargaNuevaReserva(
                "RES-1", " ", "Reunión", LocalDate.now(), LocalTime.of(9, 0),
                LocalTime.of(10, 0), List.of()));
        assertThrows(ValidacionException.class, () -> new CargaNuevaReserva(
                "RES-1", "Ana", " ", LocalDate.now(), LocalTime.of(9, 0),
                LocalTime.of(10, 0), List.of()));
        assertThrows(ValidacionException.class, () -> new CargaNuevaReserva(
                "RES-1", "Ana", "Reunión", LocalDate.now(), LocalTime.of(11, 0),
                LocalTime.of(9, 0), List.of()));
    }

    @Test
    @DisplayName("Exige tipo y carga en la notificación")
    void validaNotificacion() {
        CargaNuevaReserva carga = CargaNuevaReserva.desde(RESERVA);

        assertThrows(NullPointerException.class,
                () -> new Notificacion(null, carga));
        assertThrows(NullPointerException.class,
                () -> new Notificacion(TipoNotificacion.NUEVA_RESERVA, null));
    }
}
