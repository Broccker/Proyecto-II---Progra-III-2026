package com.sistema.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Administrador;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.EstadoReserva;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceImplReservasTest extends PruebaServicioBase {

    private static final LocalTime NUEVE = LocalTime.of(9, 0);
    private static final LocalTime ONCE = LocalTime.of(11, 0);
    private static final CategoriaRecurso PROYECTORES =
            categoria("CAT-000001", "Proyectores");

    @Test
    @DisplayName("Crea una reserva con un recurso libre de cada categoría")
    void creaReservaConRecursosLibres() {
        Funcionario ana = funcionario("222");
        Recurso proyector = recurso("1001", PROYECTORES, "Proyector Epson");
        prepararAlta(ana, List.of("CAT-000001"), List.of(proyector), Set.of());
        when(reservaDAO.siguienteId()).thenReturn("RES-000010");

        Reserva reserva = servicio.crearReserva("222", " Reunión de equipo ", HOY,
                NUEVE, ONCE, List.of("CAT-000001"));

        assertEquals("RES-000010", reserva.getId());
        assertEquals("Reunión de equipo", reserva.getActividad());
        assertEquals(HOY, reserva.getFecha());
        assertEquals(List.of("1001"), reserva.getRecursos().stream()
                .map(Recurso::getId).toList());
        assertEquals(EstadoReserva.ACTIVA, reserva.getEstado());
        verify(reservaDAO).insertar(reserva);
    }

    @Test
    @DisplayName("Elige otro recurso de la categoría cuando el primero está ocupado")
    void eligeRecursoLibreCuandoElPrimeroEstaOcupado() {
        Funcionario ana = funcionario("222");
        Recurso ocupado = recurso("1001", PROYECTORES, "Proyector Epson");
        Recurso libre = recurso("1002", PROYECTORES, "Proyector BenQ");
        prepararAlta(ana, List.of("CAT-000001"), List.of(ocupado, libre), Set.of("1001"));
        when(reservaDAO.siguienteId()).thenReturn("RES-000011");

        Reserva reserva = servicio.crearReserva("222", "Reunión", HOY, NUEVE, ONCE,
                List.of("CAT-000001"));

        assertEquals("1002", reserva.getRecursos().getFirst().getId());
    }

    @Test
    @DisplayName("Rechaza la reserva si no hay disponibilidad en alguna categoría")
    void rechazaSinDisponibilidad() {
        Funcionario ana = funcionario("222");
        prepararAlta(ana, List.of("CAT-000001"),
                List.of(recurso("1001", PROYECTORES, "Proyector")), Set.of("1001"));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.crearReserva("222", "Reunión", HOY, NUEVE, ONCE,
                        List.of("CAT-000001")));

        assertTrue(error.getMessage().contains("Proyectores"));
        verify(reservaDAO, never()).insertar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Rechaza una fecha pasada o un horario incoherente")
    void validaFechaYHorario() {
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(funcionario("222")));

        assertThrows(ValidacionException.class, () -> servicio.crearReserva("222", "Reunión",
                HOY.minusDays(1), NUEVE, ONCE, List.of("CAT-000001")));
        assertThrows(ValidacionException.class, () -> servicio.crearReserva("222", "Reunión",
                HOY, ONCE, NUEVE, List.of("CAT-000001")));
        assertThrows(ValidacionException.class, () -> servicio.crearReserva("222", "Reunión",
                HOY, null, ONCE, List.of("CAT-000001")));
        verifyNoInteractions(reservaDAO, categoriaDAO, recursoDAO);
    }

    @Test
    @DisplayName("Rechaza una reserva sin actividad o sin categorías")
    void validaCamposObligatorios() {
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(funcionario("222")));

        assertThrows(ValidacionException.class, () -> servicio.crearReserva("222", " ",
                HOY, NUEVE, ONCE, List.of("CAT-000001")));
        assertThrows(ValidacionException.class, () -> servicio.crearReserva("222", "Reunión",
                HOY, NUEVE, ONCE, List.of("  ")));
        assertThrows(ValidacionException.class, () -> servicio.crearReserva("222", "Reunión",
                HOY, NUEVE, ONCE, null));
        verifyNoInteractions(reservaDAO, categoriaDAO, recursoDAO);
    }

    @Test
    @DisplayName("Rechaza reservar a nombre de un administrador")
    void rechazaReservarComoAdministrador() {
        when(usuarioDAO.buscarPorId("111")).thenReturn(Optional.of(new Administrador("111")));

        assertThrows(NoEncontradoException.class, () -> servicio.crearReserva("111", "Reunión",
                HOY, NUEVE, ONCE, List.of("CAT-000001")));
        verifyNoInteractions(reservaDAO);
    }

    @Test
    @DisplayName("Rechaza una categoría inexistente")
    void rechazaCategoriaInexistente() {
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(funcionario("222")));
        when(categoriaDAO.buscarPorId("CAT-999999")).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> servicio.crearReserva("222", "Reunión",
                HOY, NUEVE, ONCE, List.of("CAT-999999")));
        verifyNoInteractions(reservaDAO);
    }

    @Test
    @DisplayName("Cancela una reserva futura del propio funcionario")
    void cancelaReservaFutura() {
        Funcionario ana = funcionario("222");
        Reserva reserva = reserva("RES-000001", ana, HOY, "09:00", "11:00",
                EstadoReserva.ACTIVA, recurso("1001", PROYECTORES, "Proyector"));
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(ana));
        when(reservaDAO.buscarPorId("RES-000001")).thenReturn(Optional.of(reserva));

        servicio.cancelarReserva("RES-000001", "222");

        verify(reservaDAO).cambiarEstado("RES-000001", EstadoReserva.CANCELADA);
    }

    @Test
    @DisplayName("No cancela reservas pasadas, ajenas o ya canceladas")
    void rechazaCancelacionesInvalidas() {
        Funcionario ana = funcionario("222");
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(ana));

        Reserva pasada = reserva("RES-000001", ana, HOY, "08:00", "09:00",
                EstadoReserva.ACTIVA, recurso("1001", PROYECTORES, "Proyector"));
        when(reservaDAO.buscarPorId("RES-000001")).thenReturn(Optional.of(pasada));
        assertThrows(ConflictoException.class,
                () -> servicio.cancelarReserva("RES-000001", "222"));

        Reserva cancelada = reserva("RES-000002", ana, HOY, "09:00", "11:00",
                EstadoReserva.CANCELADA, recurso("1001", PROYECTORES, "Proyector"));
        when(reservaDAO.buscarPorId("RES-000002")).thenReturn(Optional.of(cancelada));
        assertThrows(ConflictoException.class,
                () -> servicio.cancelarReserva("RES-000002", "222"));

        Reserva ajena = reserva("RES-000003", funcionario("333"), HOY, "09:00", "11:00",
                EstadoReserva.ACTIVA, recurso("1001", PROYECTORES, "Proyector"));
        when(reservaDAO.buscarPorId("RES-000003")).thenReturn(Optional.of(ajena));
        assertThrows(ConflictoException.class,
                () -> servicio.cancelarReserva("RES-000003", "222"));

        verify(reservaDAO, never()).cambiarEstado(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Rechaza cancelar una reserva inexistente")
    void rechazaCancelarInexistente() {
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(funcionario("222")));
        when(reservaDAO.buscarPorId("RES-999999")).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> servicio.cancelarReserva("RES-999999", "222"));
    }

    @Test
    @DisplayName("Lista solo las reservas no canceladas del funcionario")
    void listaReservasNoCanceladas() {
        Funcionario ana = funcionario("222");
        Reserva activa = reserva("RES-000001", ana, HOY, "09:00", "10:00",
                EstadoReserva.ACTIVA, recurso("1001", PROYECTORES, "Proyector"));
        Reserva cancelada = reserva("RES-000002", ana, HOY.plusDays(1), "09:00", "10:00",
                EstadoReserva.CANCELADA, recurso("1001", PROYECTORES, "Proyector"));
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(ana));
        when(reservaDAO.listarPorFuncionario("222")).thenReturn(List.of(activa, cancelada));

        List<Reserva> reservas = servicio.listarReservasDe("222");

        assertEquals(List.of(activa), reservas);
    }

    @Test
    @DisplayName("Lista todas las reservas y valida los rangos de fechas")
    void listaTodasYValidaRangos() {
        List<Reserva> todas = List.of(reserva("RES-000001", funcionario("222"), HOY,
                "09:00", "10:00", EstadoReserva.ACTIVA,
                recurso("1001", PROYECTORES, "Proyector")));
        when(reservaDAO.listarTodas()).thenReturn(todas);
        when(reservaDAO.listarActivasEntre(HOY, HOY.plusDays(6))).thenReturn(todas);

        assertEquals(todas, servicio.listarTodasLasReservas());
        assertEquals(todas, servicio.listarReservasActivasEntre(HOY, HOY.plusDays(6)));
        assertThrows(ValidacionException.class,
                () -> servicio.listarReservasActivasEntre(HOY.plusDays(2), HOY));
        assertThrows(ValidacionException.class,
                () -> servicio.listarReservasActivasEntre(null, HOY));
    }

    private void prepararAlta(Funcionario funcionario, List<String> categorias,
                              List<Recurso> recursos, Set<String> ocupados) {
        when(usuarioDAO.buscarPorId(funcionario.getId())).thenReturn(Optional.of(funcionario));
        when(categoriaDAO.buscarPorId(categorias.getFirst())).thenReturn(Optional.of(PROYECTORES));
        when(reservaDAO.recursosOcupados(HOY, NUEVE, ONCE)).thenReturn(ocupados);
        when(recursoDAO.listarPorCategoria(categorias.getFirst())).thenReturn(recursos);
    }
}
