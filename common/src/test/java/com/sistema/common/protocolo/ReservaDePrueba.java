package com.sistema.common.protocolo;

import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.EstadoReserva;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Reserva de ejemplo para las pruebas del protocolo.
 */
final class ReservaDePrueba {

    private ReservaDePrueba() {
    }

    static Reserva crear() {
        Funcionario funcionario = new Funcionario("222", "Ana Pérez", "88881111");
        CategoriaRecurso categoria = new CategoriaRecurso("CAT-000001", "Proyectores");
        Recurso recurso = new Recurso("1001", categoria, "Proyector Epson");
        return new Reserva("RES-000001", funcionario, "Reunión de equipo",
                LocalDate.of(2026, 3, 2), LocalTime.of(9, 0), LocalTime.of(11, 0),
                List.of(recurso), EstadoReserva.ACTIVA);
    }
}
