package com.sistema.backend.server;

import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.EstadoReserva;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import com.sistema.common.protocolo.Notificacion;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos compartidos por las pruebas del servidor.
 */
final class DatosPrueba {

    static final String ID_FUNCIONARIO = "222";
    static final String ID_CATEGORIA = "CAT-000001";
    static final String ID_RESERVA = "RES-000001";
    static final LocalDate FECHA = LocalDate.of(2026, 11, 2);
    static final LocalTime HORA_INICIO = LocalTime.of(9, 0);
    static final LocalTime HORA_FIN = LocalTime.of(11, 0);
    static final String ACTIVIDAD = "Reunión de equipo";
    static final String ENCARGADO = "Ana Pérez";

    private DatosPrueba() {
    }

    static Reserva reserva() {
        Funcionario funcionario = new Funcionario(ID_FUNCIONARIO, ENCARGADO, "88881111");
        CategoriaRecurso categoria = new CategoriaRecurso(ID_CATEGORIA, "Proyectores");
        Recurso recurso = new Recurso("1001", categoria, "Proyector Epson");
        return new Reserva(ID_RESERVA, funcionario, ACTIVIDAD, FECHA, HORA_INICIO, HORA_FIN,
                List.of(recurso), EstadoReserva.ACTIVA);
    }

    static Notificacion notificacion() {
        return Notificacion.nuevaReserva(reserva());
    }
}
