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

/**
 * Contrato que el frontend consume y el backend implementa.
 *
 * <p>El frontend nunca accede a la base de datos: todas estas operaciones
 * viajan por sockets hasta el {@code ServiceImpl} del backend (Fase 5).</p>
 *
 * <p>Las credenciales de la IA y de MySQL quedan solo del lado del servidor.
 * El frontend recibe datos ya validados.</p>
 */
public interface Service {

    // ------------------------------------------------------------------
    // Sesión
    // ------------------------------------------------------------------

    /**
     * Inicia sesión y devuelve el usuario autenticado (sin su clave).
     *
     * @throws com.sistema.common.excepciones.AutenticacionException si las
     *         credenciales son incorrectas
     */
    Usuario iniciarSesion(String id, String clave);

    /**
     * Cambia la clave de un usuario verificando primero la clave actual.
     */
    void cambiarClave(String id, String claveActual, String nuevaClave, String confirmacion);

    // ------------------------------------------------------------------
    // Funcionarios
    // ------------------------------------------------------------------

    void altaFuncionario(String id, String nombre, String telefono);

    void modificarFuncionario(String id, String nombre, String telefono);

    void eliminarFuncionario(String id);

    List<Funcionario> listarFuncionarios();

    List<Funcionario> buscarFuncionarios(String criterio);

    // ------------------------------------------------------------------
    // Categorías de recursos
    // ------------------------------------------------------------------

    /**
     * Crea una categoría con el siguiente identificador disponible.
     */
    CategoriaRecurso altaCategoria(String descripcion);

    /**
     * Modifica una categoría. Si {@code nuevoId} viene vacío o nulo se
     * conserva el identificador actual.
     */
    void modificarCategoria(String id, String nuevoId, String descripcion);

    void eliminarCategoria(String id);

    List<CategoriaRecurso> listarCategorias();

    List<CategoriaRecurso> buscarCategorias(String criterio);

    boolean existeDescripcionCategoria(String descripcion);

    // ------------------------------------------------------------------
    // Recursos
    // ------------------------------------------------------------------

    void altaRecurso(String id, String categoriaId, String descripcion);

    /**
     * Modifica un recurso. Si {@code nuevoId} viene vacío o nulo se conserva
     * el identificador actual.
     */
    void modificarRecurso(String id, String nuevoId, String categoriaId, String descripcion);

    void eliminarRecurso(String id);

    List<Recurso> listarRecursos();

    List<Recurso> buscarRecursos(String criterio);

    List<Recurso> filtrarRecursos(String categoriaId, String criterio);

    // ------------------------------------------------------------------
    // Reservas
    // ------------------------------------------------------------------

    /**
     * Crea una reserva eligiendo automáticamente un recurso disponible de
     * cada categoría seleccionada.
     */
    Reserva crearReserva(String funcionarioId, String actividad, LocalDate fecha,
                         LocalTime horaInicio, LocalTime horaFin, List<String> idsCategorias);

    /**
     * Cancela una reserva futura del funcionario indicado.
     */
    void cancelarReserva(String idReserva, String funcionarioId);

    /**
     * Reservas no canceladas de un funcionario.
     */
    List<Reserva> listarReservasDe(String funcionarioId);

    List<Reserva> listarTodasLasReservas();

    List<Reserva> listarReservasActivasEntre(LocalDate desde, LocalDate hasta);

    // ------------------------------------------------------------------
    // Actividades y calendarización
    // ------------------------------------------------------------------

    MatrizActividades construirMatrizActividades(LocalDate fecha);

    MatrizCalendarizacion construirMatrizCalendarizacion(LocalDate fecha, String categoriaId);

    // ------------------------------------------------------------------
    // Estadísticas
    // ------------------------------------------------------------------

    List<ConteoCategoria> categoriasReservadas(LocalDate desde, LocalDate hasta);

    List<ConteoSemana> actividadesPorSemana(LocalDate desde, LocalDate hasta);

    // ------------------------------------------------------------------
    // Extracción con IA
    // ------------------------------------------------------------------

    /**
     * Interpreta una frase libre y propone los datos de una reserva.
     * La clave del proveedor de IA nunca sale del backend.
     */
    DatosReservaSugerida extraerDatosReserva(String frase);
}
