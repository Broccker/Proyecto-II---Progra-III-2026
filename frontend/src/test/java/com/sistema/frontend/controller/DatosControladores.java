package com.sistema.frontend.controller;

import com.sistema.common.dto.CeldaCalendario;
import com.sistema.common.dto.MatrizActividades;
import com.sistema.common.dto.MatrizCalendarizacion;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.EstadoReserva;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos de ejemplo compartidos por las pruebas de controladores.
 */
final class DatosControladores {

    static final String ID_FUNCIONARIO = "222";
    static final String NOMBRE_FUNCIONARIO = "Ana Pérez";
    static final String TELEFONO = "88881111";
    static final String ID_CATEGORIA = "CAT-000001";
    static final String DESCRIPCION_CATEGORIA = "Proyectores";
    static final String ID_RECURSO = "1001";
    static final String DESCRIPCION_RECURSO = "Proyector Epson";
    static final String ID_RESERVA = "RES-000001";
    static final String ACTIVIDAD = "Reunión de equipo";
    static final LocalDate FECHA = LocalDate.of(2026, 11, 2);
    static final LocalTime INICIO = LocalTime.of(9, 0);
    static final LocalTime FIN = LocalTime.of(11, 0);

    private DatosControladores() {
    }

    static Funcionario funcionario() {
        return new Funcionario(ID_FUNCIONARIO, NOMBRE_FUNCIONARIO, TELEFONO);
    }

    static Funcionario otroFuncionario() {
        return new Funcionario("333", "Luis Rojas", "77776666");
    }

    static CategoriaRecurso categoria() {
        return new CategoriaRecurso(ID_CATEGORIA, DESCRIPCION_CATEGORIA);
    }

    static CategoriaRecurso otraCategoria() {
        return new CategoriaRecurso("CAT-000002", "Salas");
    }

    static Recurso recurso() {
        return new Recurso(ID_RECURSO, categoria(), DESCRIPCION_RECURSO);
    }

    static Reserva reserva() {
        return new Reserva(ID_RESERVA, funcionario(), ACTIVIDAD, FECHA, INICIO, FIN,
                List.of(recurso()), EstadoReserva.ACTIVA);
    }

    static MatrizCalendarizacion matrizCalendarizacion() {
        List<List<CeldaCalendario>> celdas = List.of(
                List.of(new CeldaCalendario(ACTIVIDAD, true)),
                List.of(CeldaCalendario.libre()));
        return new MatrizCalendarizacion(FECHA, categoria(), List.of(recurso()),
                List.of(LocalTime.of(9, 0), LocalTime.of(10, 0)), celdas);
    }

    static MatrizActividades matrizActividades() {
        List<List<CeldaCalendario>> celdas = List.of(
                List.of(new CeldaCalendario(ACTIVIDAD, true), CeldaCalendario.libre()),
                List.of(CeldaCalendario.libre(), CeldaCalendario.libre()));
        return new MatrizActividades(FECHA, List.of(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                List.of("Lunes", "Martes"), celdas);
    }
}
