package com.sistema.backend.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.EstadoReserva;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReservaDAOIT extends PruebaBDBase {

    private static final LocalDate DIA = LocalDate.of(2026, 3, 2);

    private ReservaDAO dao() {
        return new ReservaDAO(fuente());
    }

    @Test
    @DisplayName("Crea una reserva con funcionario y varios recursos")
    void insertaYRecuperaReservaCompleta() {
        Funcionario funcionario = crearFuncionario("222", "Ana Pérez", "88881111");
        CategoriaRecurso proyectores = crearCategoria("CAT-000001", "Proyectores");
        CategoriaRecurso salas = crearCategoria("CAT-000002", "Salas");
        Recurso proyector = crearRecurso("1001", proyectores, "Proyector Epson");
        Recurso sala = crearRecurso("2001", salas, "Sala A");

        insertarReserva("RES-000001", funcionario, DIA, "09:00", "11:00", proyector, sala);

        Reserva reserva = dao().buscarPorId("RES-000001").orElseThrow();
        assertEquals("Actividad RES-000001", reserva.getActividad());
        assertEquals(DIA, reserva.getFecha());
        assertEquals(LocalTime.of(9, 0), reserva.getHoraInicio());
        assertEquals(LocalTime.of(11, 0), reserva.getHoraFin());
        assertEquals(EstadoReserva.ACTIVA, reserva.getEstado());
        assertEquals("Ana Pérez", reserva.getFuncionario().getNombre());
        assertEquals(Set.of("1001", "2001"), idsRecursos(reserva));
        assertEquals(1, dao().listarTodas().size());
    }

    @Test
    @DisplayName("Rechaza un traslape y no deja datos a medias")
    void rechazaTraslapeSinDejarRestos() {
        Funcionario funcionario = crearFuncionario("222", "Ana Pérez", "88881111");
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        Recurso recurso = crearRecurso("1001", categoria, "Proyector Epson");
        insertarReserva("RES-000001", funcionario, DIA, "09:00", "11:00", recurso);

        Reserva segunda = new Reserva("RES-000002", funcionario, "Otra actividad", DIA,
                LocalTime.of(10, 0), LocalTime.of(12, 0), List.of(recurso),
                EstadoReserva.ACTIVA);

        assertThrows(ConflictoException.class, () -> dao().insertar(segunda));
        assertEquals(1, contar("reservas"));
        assertEquals(1, contar("reserva_recursos"));
    }

    @Test
    @DisplayName("Permite reservas adyacentes sin traslape")
    void permiteHorariosAdyacentes() {
        Funcionario funcionario = crearFuncionario("222", "Ana Pérez", "88881111");
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        Recurso recurso = crearRecurso("1001", categoria, "Proyector Epson");

        insertarReserva("RES-000001", funcionario, DIA, "09:00", "10:00", recurso);
        assertFalse(dao().existeTraslape("1001", DIA,
                LocalTime.of(10, 0), LocalTime.of(11, 0)));

        insertarReserva("RES-000002", funcionario, DIA, "10:00", "11:00", recurso);

        assertEquals(2, contar("reservas"));
        assertEquals(Set.of("1001"), dao().recursosOcupados(DIA,
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
    }

    @Test
    @DisplayName("Permite el mismo horario con recursos distintos o en otra fecha")
    void permiteRecursosOFechasDistintas() {
        Funcionario funcionario = crearFuncionario("222", "Ana Pérez", "88881111");
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        Recurso primero = crearRecurso("1001", categoria, "Proyector Epson");
        Recurso segundo = crearRecurso("1002", categoria, "Proyector ViewSonic");

        insertarReserva("RES-000001", funcionario, DIA, "09:00", "10:00", primero);
        insertarReserva("RES-000002", funcionario, DIA, "09:00", "10:00", segundo);
        insertarReserva("RES-000003", funcionario, DIA.plusDays(1), "09:00", "10:00", primero);

        assertEquals(3, contar("reservas"));
    }

    @Test
    @DisplayName("Rechaza un recurso inexistente")
    void rechazaRecursoInexistente() {
        Funcionario funcionario = crearFuncionario("222", "Ana Pérez", "88881111");
        CategoriaRecurso categoria = new CategoriaRecurso("CAT-999999", "Fantasma");
        Recurso fantasma = new Recurso("9999", categoria, "No existe");
        Reserva reserva = new Reserva("RES-000001", funcionario, "Prueba", DIA,
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(fantasma),
                EstadoReserva.ACTIVA);

        assertThrows(ConflictoException.class, () -> dao().insertar(reserva));
        assertEquals(0, contar("reservas"));
    }

    @Test
    @DisplayName("Cancelar libera el recurso para el mismo horario")
    void cancelarLiberaElRecurso() {
        Funcionario funcionario = crearFuncionario("222", "Ana Pérez", "88881111");
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        Recurso recurso = crearRecurso("1001", categoria, "Proyector Epson");
        insertarReserva("RES-000001", funcionario, DIA, "09:00", "11:00", recurso);
        assertTrue(dao().existeTraslape("1001", DIA,
                LocalTime.of(10, 0), LocalTime.of(12, 0)));

        dao().cambiarEstado("RES-000001", EstadoReserva.CANCELADA);

        assertEquals(EstadoReserva.CANCELADA,
                dao().buscarPorId("RES-000001").orElseThrow().getEstado());
        assertFalse(dao().existeTraslape("1001", DIA,
                LocalTime.of(10, 0), LocalTime.of(12, 0)));
        insertarReserva("RES-000002", funcionario, DIA, "10:00", "12:00", recurso);
        assertEquals(2, contar("reservas"));
    }

    @Test
    @DisplayName("Cambiar el estado de una reserva inexistente falla")
    void cambiarEstadoInexistente() {
        assertThrows(NoEncontradoException.class,
                () -> dao().cambiarEstado("RES-999999", EstadoReserva.CANCELADA));
    }

    @Test
    @DisplayName("Eliminar la reserva borra su detalle en cascada")
    void eliminarBorraDetalle() {
        Funcionario funcionario = crearFuncionario("222", "Ana Pérez", "88881111");
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        Recurso primero = crearRecurso("1001", categoria, "Proyector Epson");
        Recurso segundo = crearRecurso("1002", categoria, "Proyector ViewSonic");
        insertarReserva("RES-000001", funcionario, DIA, "09:00", "11:00", primero, segundo);

        dao().eliminar("RES-000001");

        assertEquals(0, contar("reservas"));
        assertEquals(0, contar("reserva_recursos"));
        assertThrows(NoEncontradoException.class, () -> dao().eliminar("RES-000001"));
    }

    @Test
    @DisplayName("Lista reservas por rango, fecha y funcionario")
    void listaPorRangosYFuncionario() {
        Funcionario ana = crearFuncionario("222", "Ana Pérez", "88881111");
        Funcionario luis = crearFuncionario("333", "Luis Rojas", "88882222");
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Proyectores");
        Recurso recurso = crearRecurso("1001", categoria, "Proyector Epson");

        insertarReserva("RES-000001", ana, DIA, "09:00", "10:00", recurso);
        insertarReserva("RES-000002", luis, DIA.plusDays(2), "09:00", "10:00", recurso);
        Reserva cancelada = insertarReserva("RES-000003", ana, DIA.plusDays(4),
                "09:00", "10:00", recurso);
        dao().cambiarEstado(cancelada.getId(), EstadoReserva.CANCELADA);

        assertEquals(2, dao().listarActivasEntre(DIA, DIA.plusDays(6)).size());
        assertEquals(1, dao().listarActivasEnFecha(DIA).size());
        assertEquals(2, dao().listarPorFuncionario("222").size());
        assertEquals(2, dao().listarActivas().size());
        assertEquals(2, dao().contarActivasEntre(DIA, DIA.plusDays(3)));
    }

    @Test
    @DisplayName("Cuenta reservas por categoría y actividades por semana")
    void estadisticas() {
        Funcionario funcionario = crearFuncionario("222", "Ana Pérez", "88881111");
        CategoriaRecurso proyectores = crearCategoria("CAT-000001", "Proyectores");
        CategoriaRecurso salas = crearCategoria("CAT-000002", "Salas");
        Recurso proyector = crearRecurso("1001", proyectores, "Proyector Epson");
        Recurso sala = crearRecurso("2001", salas, "Sala A");

        insertarReserva("RES-000001", funcionario, LocalDate.of(2026, 3, 2),
                "09:00", "10:00", proyector);
        insertarReserva("RES-000002", funcionario, LocalDate.of(2026, 3, 3),
                "09:00", "10:00", proyector);
        insertarReserva("RES-000003", funcionario, LocalDate.of(2026, 3, 9),
                "09:00", "10:00", sala);
        Reserva cancelada = insertarReserva("RES-000004", funcionario,
                LocalDate.of(2026, 3, 10), "09:00", "10:00", sala);
        dao().cambiarEstado(cancelada.getId(), EstadoReserva.CANCELADA);

        List<ConteoCategoria> categorias = dao().contarReservasPorCategoria(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15));
        assertEquals(2, categorias.size());
        assertEquals("CAT-000001", categorias.getFirst().categoria().getId());
        assertEquals(2, categorias.getFirst().cantidad());
        assertEquals(1, categorias.getLast().cantidad());

        List<ConteoSemana> semanas = dao().contarActividadesPorSemana(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15));
        assertEquals(2, semanas.size());
        assertEquals(LocalDate.of(2026, 3, 2), semanas.getFirst().lunes());
        assertEquals(2, semanas.getFirst().cantidad());
        assertEquals(LocalDate.of(2026, 3, 9), semanas.getLast().lunes());
        assertEquals(1, semanas.getLast().cantidad());
    }

    @Test
    @DisplayName("Rechaza rangos de fechas y horarios inválidos")
    void rechazaRangosYHorariosInvalidos() {
        assertThrows(ValidacionException.class,
                () -> dao().listarActivasEntre(DIA.plusDays(2), DIA));
        assertThrows(ValidacionException.class,
                () -> dao().contarReservasPorCategoria(DIA.plusDays(2), DIA));
        assertThrows(ValidacionException.class,
                () -> dao().recursosOcupados(DIA, LocalTime.of(11, 0), LocalTime.of(10, 0)));
    }

    private static Set<String> idsRecursos(Reserva reserva) {
        return reserva.getRecursos().stream()
                .map(Recurso::getId)
                .collect(java.util.stream.Collectors.toSet());
    }
}
