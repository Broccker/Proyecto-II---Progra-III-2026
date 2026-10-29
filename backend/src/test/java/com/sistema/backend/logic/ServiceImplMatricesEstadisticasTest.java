package com.sistema.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import com.sistema.common.dto.MatrizActividades;
import com.sistema.common.dto.MatrizCalendarizacion;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.EstadoReserva;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceImplMatricesEstadisticasTest extends PruebaServicioBase {

    private static final CategoriaRecurso PROYECTORES =
            categoria("CAT-000001", "Proyectores");

    @Test
    @DisplayName("Construye la matriz semanal de actividades desde el lunes")
    void construyeMatrizActividades() {
        Funcionario ana = funcionario("222");
        Reserva reserva = reserva("RES-000001", ana, HOY, "09:00", "10:00",
                EstadoReserva.ACTIVA, recurso("1001", PROYECTORES, "Proyector"));
        when(reservaDAO.listarActivasEntre(HOY, HOY.plusDays(6))).thenReturn(List.of(reserva));

        MatrizActividades matriz = servicio.construirMatrizActividades(HOY.plusDays(2));

        assertEquals(HOY, matriz.lunes());
        assertEquals(24, matriz.horas().size());
        assertEquals(7, matriz.dias().size());
        assertEquals("Actividad RES-000001 (Ana Pérez)", matriz.aTexto()[9][0]);
        assertTrue(matriz.celdas().get(9).getFirst().ocupada());
        assertFalse(matriz.celdas().get(8).getFirst().ocupada());
        assertEquals("", matriz.aTexto()[8][1]);
    }

    @Test
    @DisplayName("La matriz semanal queda vacía si no hay reservas")
    void matrizActividadesVacia() {
        when(reservaDAO.listarActivasEntre(HOY, HOY.plusDays(6))).thenReturn(List.of());

        MatrizActividades matriz = servicio.construirMatrizActividades(HOY);

        assertTrue(matriz.celdas().stream().flatMap(List::stream)
                .noneMatch(celda -> celda.ocupada()));
    }

    @Test
    @DisplayName("Construye la matriz de calendarización de una categoría")
    void construyeMatrizCalendarizacion() {
        Funcionario ana = funcionario("222");
        Recurso proyector = recurso("1001", PROYECTORES, "Proyector Epson");
        Reserva reserva = reserva("RES-000001", ana, HOY, "09:00", "10:00",
                EstadoReserva.ACTIVA, proyector);
        when(categoriaDAO.buscarPorId("CAT-000001")).thenReturn(Optional.of(PROYECTORES));
        when(recursoDAO.listarPorCategoria("CAT-000001")).thenReturn(List.of(proyector));
        when(reservaDAO.listarActivasEnFecha(HOY)).thenReturn(List.of(reserva));

        MatrizCalendarizacion matriz =
                servicio.construirMatrizCalendarizacion(HOY, "CAT-000001");

        assertEquals(HOY, matriz.fecha());
        assertEquals("CAT-000001", matriz.categoria().getId());
        assertEquals(1, matriz.recursos().size());
        assertEquals("Actividad RES-000001 — Ana Pérez", matriz.aTexto()[9][0]);
        assertTrue(matriz.celdas().get(9).getFirst().ocupada());
        assertEquals("", matriz.aTexto()[10][0]);
    }

    @Test
    @DisplayName("Rechaza matrix sin fecha o de una categoría sin recursos")
    void rechazaMatricesInvalidas() {
        assertThrows(ValidacionException.class, () -> servicio.construirMatrizActividades(null));
        assertThrows(ValidacionException.class,
                () -> servicio.construirMatrizCalendarizacion(null, "CAT-000001"));

        when(categoriaDAO.buscarPorId("CAT-000001")).thenReturn(Optional.of(PROYECTORES));
        when(recursoDAO.listarPorCategoria("CAT-000001")).thenReturn(List.of());
        assertThrows(ValidacionException.class,
                () -> servicio.construirMatrizCalendarizacion(HOY, "CAT-000001"));
    }

    @Test
    @DisplayName("Delega las estadísticas en el DAO y valida el rango")
    void calculaEstadisticas() {
        List<ConteoCategoria> categorias =
                List.of(new ConteoCategoria(PROYECTORES, 3));
        List<ConteoSemana> semanas =
                List.of(new ConteoSemana(HOY, 5));
        when(reservaDAO.contarReservasPorCategoria(HOY, HOY.plusDays(30)))
                .thenReturn(categorias);
        when(reservaDAO.contarActividadesPorSemana(HOY, HOY.plusDays(30)))
                .thenReturn(semanas);

        assertEquals(categorias,
                servicio.categoriasReservadas(HOY, HOY.plusDays(30)));
        assertEquals(semanas,
                servicio.actividadesPorSemana(HOY, HOY.plusDays(30)));
    }

    @Test
    @DisplayName("Rechaza rangos de estadísticas inválidos antes de consultar")
    void rechazaRangosInvalidos() {
        assertThrows(ValidacionException.class,
                () -> servicio.categoriasReservadas(HOY.plusDays(1), HOY));
        assertThrows(ValidacionException.class,
                () -> servicio.actividadesPorSemana(null, HOY));
    }
}
