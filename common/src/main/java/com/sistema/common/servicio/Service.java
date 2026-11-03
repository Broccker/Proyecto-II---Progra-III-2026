package com.sistema.common.servicio;

import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import com.sistema.common.dto.DatosReservaSugerida;
import com.sistema.common.dto.MatrizActividades;
import com.sistema.common.dto.MatrizCalendarizacion;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import com.sistema.common.modelo.Usuario;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


public interface Service {


    Usuario iniciarSesion(String id, String clave);


    void cambiarClave(String id, String claveActual, String nuevaClave, String confirmacion);


    void altaFuncionario(String id, String nombre, String telefono);

    void modificarFuncionario(String id, String nombre, String telefono);

    void eliminarFuncionario(String id);

    List<Funcionario> listarFuncionarios();

    List<Funcionario> buscarFuncionarios(String criterio);


    CategoriaRecurso altaCategoria(String descripcion);


    void modificarCategoria(String id, String nuevoId, String descripcion);

    void eliminarCategoria(String id);

    List<CategoriaRecurso> listarCategorias();

    List<CategoriaRecurso> buscarCategorias(String criterio);

    boolean existeDescripcionCategoria(String descripcion);


    void altaRecurso(String id, String categoriaId, String descripcion);


    void modificarRecurso(String id, String nuevoId, String categoriaId, String descripcion);

    void eliminarRecurso(String id);

    List<Recurso> listarRecursos();

    List<Recurso> buscarRecursos(String criterio);

    List<Recurso> filtrarRecursos(String categoriaId, String criterio);


    Reserva crearReserva(String funcionarioId, String actividad, LocalDate fecha,
                         LocalTime horaInicio, LocalTime horaFin, List<String> idsCategorias);


    void cancelarReserva(String idReserva, String funcionarioId);


    List<Reserva> listarReservasDe(String funcionarioId);

    List<Reserva> listarTodasLasReservas();

    List<Reserva> listarReservasActivasEntre(LocalDate desde, LocalDate hasta);


    MatrizActividades construirMatrizActividades(LocalDate fecha);

    MatrizCalendarizacion construirMatrizCalendarizacion(LocalDate fecha, String categoriaId);


    List<ConteoCategoria> categoriasReservadas(LocalDate desde, LocalDate hasta);

    List<ConteoSemana> actividadesPorSemana(LocalDate desde, LocalDate hasta);


    DatosReservaSugerida extraerDatosReserva(String frase);
}
