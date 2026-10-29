package com.sistema.backend.data;

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
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Acceso a las tablas {@code reservas} y {@code reserva_recursos}.
 *
 * <p>Crear una reserva es una transacción: se bloquean los recursos
 * seleccionados con {@code SELECT ... FOR UPDATE}, se comprueba que no haya
 * traslapes con reservas activas y recién entonces se inserta la reserva y su
 * detalle. Si algo falla no queda ningún dato a medias.</p>
 */
public final class ReservaDAO extends DAOBase {

    private static final String SELECT_BASE = """
            SELECT r.id, r.funcionario_id, u.nombre AS funcionario_nombre,
                   u.telefono AS funcionario_telefono, r.actividad, r.fecha,
                   r.hora_inicio, r.hora_fin, r.estado
            FROM reservas r
            JOIN usuarios u ON u.id = r.funcionario_id
            """;

    public ReservaDAO(FuenteDatos fuente) {
        super(fuente);
    }

    public List<Reserva> listarTodas() {
        return consultar(SELECT_BASE + " ORDER BY r.fecha, r.hora_inicio, r.id", List.of());
    }

    public List<Reserva> listarActivas() {
        return consultar(SELECT_BASE
                + " WHERE r.estado = 'ACTIVA' ORDER BY r.fecha, r.hora_inicio, r.id", List.of());
    }

    public List<Reserva> listarPorFuncionario(String funcionarioId) {
        Objects.requireNonNull(funcionarioId, "El funcionario es obligatorio.");
        return consultar(SELECT_BASE
                + " WHERE r.funcionario_id = ? ORDER BY r.fecha, r.hora_inicio, r.id",
                List.of(funcionarioId));
    }

    public List<Reserva> listarActivasEntre(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return consultar(SELECT_BASE
                + " WHERE r.estado = 'ACTIVA' AND r.fecha BETWEEN ? AND ?"
                + " ORDER BY r.fecha, r.hora_inicio, r.id",
                List.of(desde, hasta));
    }

    public List<Reserva> listarActivasEnFecha(LocalDate fecha) {
        Objects.requireNonNull(fecha, "La fecha es obligatoria.");
        return consultar(SELECT_BASE
                + " WHERE r.estado = 'ACTIVA' AND r.fecha = ?"
                + " ORDER BY r.hora_inicio, r.id",
                List.of(fecha));
    }

    public Optional<Reserva> buscarPorId(String id) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        List<Reserva> reservas = consultar(SELECT_BASE + " WHERE r.id = ?", List.of(id));
        return reservas.isEmpty() ? Optional.empty() : Optional.of(reservas.getFirst());
    }

    /**
     * Inserta la reserva y su detalle de recursos de forma atómica.
     *
     * @throws ConflictoException si algún recurso ya está reservado en ese
     *                            horario o si el identificador ya existe
     */
    public void insertar(Reserva reserva) {
        Objects.requireNonNull(reserva, "La reserva es obligatoria.");
        Set<String> idsRecursos = new LinkedHashSet<>();
        for (Recurso recurso : reserva.getRecursos()) {
            idsRecursos.add(recurso.getId());
        }
        enTransaccion("crear reserva", conexion -> {
            bloquearRecursos(conexion, idsRecursos);
            Set<String> ocupados = recursosOcupados(conexion, reserva.getFecha(),
                    reserva.getHoraInicio(), reserva.getHoraFin(), idsRecursos);
            if (!ocupados.isEmpty()) {
                throw new ConflictoException(
                        "No hay disponibilidad: los siguientes recursos ya están reservados "
                                + "en ese horario: " + describirRecursos(reserva, ocupados) + ".");
            }

            try (PreparedStatement sentencia = conexion.prepareStatement("""
                    INSERT INTO reservas
                        (id, funcionario_id, actividad, fecha, hora_inicio, hora_fin, estado)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                sentencia.setString(1, reserva.getId());
                sentencia.setString(2, reserva.getFuncionario().getId());
                sentencia.setString(3, reserva.getActividad());
                sentencia.setDate(4, Date.valueOf(reserva.getFecha()));
                sentencia.setTime(5, Time.valueOf(reserva.getHoraInicio()));
                sentencia.setTime(6, Time.valueOf(reserva.getHoraFin()));
                sentencia.setString(7, reserva.getEstado().name());
                sentencia.executeUpdate();
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO reserva_recursos (reserva_id, recurso_id) VALUES (?, ?)")) {
                for (String idRecurso : idsRecursos) {
                    sentencia.setString(1, reserva.getId());
                    sentencia.setString(2, idRecurso);
                    sentencia.addBatch();
                }
                sentencia.executeBatch();
            }
            return null;
        });
    }

    public void cambiarEstado(String id, EstadoReserva estado) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        Objects.requireNonNull(estado, "El estado es obligatorio.");
        enTransaccion("cambiar estado de la reserva", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "UPDATE reservas SET estado = ? WHERE id = ?")) {
                sentencia.setString(1, estado.name());
                sentencia.setString(2, id);
                if (sentencia.executeUpdate() == 0) {
                    throw new NoEncontradoException(
                            "No existe la reserva con identificador " + id + ".");
                }
            }
            return null;
        });
    }

    /**
     * Elimina la reserva; el detalle se borra en cascada.
     */
    public void eliminar(String id) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        enTransaccion("eliminar reserva", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "DELETE FROM reservas WHERE id = ?")) {
                sentencia.setString(1, id);
                if (sentencia.executeUpdate() == 0) {
                    throw new NoEncontradoException(
                            "No existe la reserva con identificador " + id + ".");
                }
            }
            return null;
        });
    }

    /**
     * Identificadores de los recursos ocupados por reservas activas que se
     * traslapan con el horario indicado.
     */
    public Set<String> recursosOcupados(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        Objects.requireNonNull(fecha, "La fecha es obligatoria.");
        validarHorario(horaInicio, horaFin);
        return enConsulta("consultar disponibilidad", conexion ->
                recursosOcupados(conexion, fecha, horaInicio, horaFin, null));
    }

    public boolean existeTraslape(String recursoId, LocalDate fecha,
                                  LocalTime horaInicio, LocalTime horaFin) {
        Objects.requireNonNull(recursoId, "El recurso es obligatorio.");
        return recursosOcupados(fecha, horaInicio, horaFin).contains(recursoId);
    }

    public long contarActivasEntre(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return enConsulta("contar reservas activas", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT COUNT(*) FROM reservas "
                            + "WHERE estado = 'ACTIVA' AND fecha BETWEEN ? AND ?")) {
                sentencia.setDate(1, Date.valueOf(desde));
                sentencia.setDate(2, Date.valueOf(hasta));
                try (ResultSet filas = sentencia.executeQuery()) {
                    filas.next();
                    return filas.getLong(1);
                }
            }
        });
    }

    /**
     * Cantidad de reservas activas por categoría en un rango de fechas.
     * Cada reserva cuenta una sola vez por categoría, aunque use varios
     * recursos de esa categoría.
     */
    public List<ConteoCategoria> contarReservasPorCategoria(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return enConsulta("contar reservas por categoría", conexion -> {
            String sql = """
                    SELECT c.id AS categoria_id, c.descripcion AS categoria_descripcion,
                           COUNT(DISTINCT rr.reserva_id) AS cantidad
                    FROM categorias c
                    JOIN recursos rc ON rc.categoria_id = c.id
                    JOIN reserva_recursos rr ON rr.recurso_id = rc.id
                    JOIN reservas r ON r.id = rr.reserva_id
                    WHERE r.estado = 'ACTIVA' AND r.fecha BETWEEN ? AND ?
                    GROUP BY c.id, c.descripcion
                    ORDER BY cantidad DESC, c.descripcion
                    """;
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setDate(1, Date.valueOf(desde));
                sentencia.setDate(2, Date.valueOf(hasta));
                try (ResultSet filas = sentencia.executeQuery()) {
                    List<ConteoCategoria> conteos = new ArrayList<>();
                    while (filas.next()) {
                        CategoriaRecurso categoria = new CategoriaRecurso(
                                filas.getString("categoria_id"),
                                filas.getString("categoria_descripcion"));
                        conteos.add(new ConteoCategoria(categoria, filas.getLong("cantidad")));
                    }
                    return conteos;
                }
            }
        });
    }

    /**
     * Cantidad de actividades (reservas activas) por semana; la semana se
     * identifica con su lunes.
     */
    public List<ConteoSemana> contarActividadesPorSemana(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return enConsulta("contar actividades por semana", conexion -> {
            String sql = """
                    SELECT DATE_SUB(fecha, INTERVAL WEEKDAY(fecha) DAY) AS lunes,
                           COUNT(*) AS cantidad
                    FROM reservas
                    WHERE estado = 'ACTIVA' AND fecha BETWEEN ? AND ?
                    GROUP BY lunes
                    ORDER BY lunes
                    """;
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setDate(1, Date.valueOf(desde));
                sentencia.setDate(2, Date.valueOf(hasta));
                try (ResultSet filas = sentencia.executeQuery()) {
                    List<ConteoSemana> conteos = new ArrayList<>();
                    while (filas.next()) {
                        conteos.add(new ConteoSemana(
                                filas.getDate("lunes").toLocalDate(),
                                filas.getLong("cantidad")));
                    }
                    return conteos;
                }
            }
        });
    }

    private List<Reserva> consultar(String sql, List<Object> parametros) {
        return enConsulta("consultar reservas", conexion -> {
            List<FilaReserva> filasReserva = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                for (int i = 0; i < parametros.size(); i++) {
                    sentencia.setObject(i + 1, parametros.get(i));
                }
                try (ResultSet filas = sentencia.executeQuery()) {
                    while (filas.next()) {
                        filasReserva.add(mapearFila(filas));
                    }
                }
            }
            if (filasReserva.isEmpty()) {
                return List.of();
            }
            List<String> ids = filasReserva.stream().map(FilaReserva::id).toList();
            Map<String, List<Recurso>> recursosPorReserva = cargarRecursos(conexion, ids);
            List<Reserva> reservas = new ArrayList<>(filasReserva.size());
            for (FilaReserva fila : filasReserva) {
                reservas.add(new Reserva(fila.id(), fila.funcionario(), fila.actividad(),
                        fila.fecha(), fila.horaInicio(), fila.horaFin(),
                        recursosPorReserva.getOrDefault(fila.id(), List.of()), fila.estado()));
            }
            return reservas;
        });
    }

    private static Map<String, List<Recurso>> cargarRecursos(Connection conexion, List<String> idsReserva)
            throws SQLException {
        String marcadores = String.join(", ", java.util.Collections.nCopies(idsReserva.size(), "?"));
        String sql = """
                SELECT rr.reserva_id, rec.id, rec.descripcion, cat.id AS categoria_id,
                       cat.descripcion AS categoria_descripcion
                FROM reserva_recursos rr
                JOIN recursos rec ON rec.id = rr.recurso_id
                JOIN categorias cat ON cat.id = rec.categoria_id
                WHERE rr.reserva_id IN (%s)
                ORDER BY rr.reserva_id, rec.id
                """.formatted(marcadores);
        Map<String, List<Recurso>> recursos = new LinkedHashMap<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (int i = 0; i < idsReserva.size(); i++) {
                sentencia.setString(i + 1, idsReserva.get(i));
            }
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    CategoriaRecurso categoria = new CategoriaRecurso(
                            filas.getString("categoria_id"),
                            filas.getString("categoria_descripcion"));
                    Recurso recurso = new Recurso(filas.getString("id"), categoria,
                            filas.getString("descripcion"));
                    recursos.computeIfAbsent(filas.getString("reserva_id"),
                            clave -> new ArrayList<>()).add(recurso);
                }
            }
        }
        return recursos;
    }

    /**
     * Bloquea las filas de los recursos seleccionados hasta que termine la
     * transacción. Dos reservas simultáneas sobre el mismo recurso se
     * serializan y la segunda vuelve a comprobar los traslapes.
     */
    private static void bloquearRecursos(Connection conexion, Set<String> idsRecursos)
            throws SQLException {
        String marcadores = String.join(", ", java.util.Collections.nCopies(idsRecursos.size(), "?"));
        String sql = "SELECT id FROM recursos WHERE id IN (" + marcadores + ") ORDER BY id FOR UPDATE";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            int indice = 1;
            for (String id : idsRecursos) {
                sentencia.setString(indice++, id);
            }
            Set<String> encontrados = new LinkedHashSet<>();
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    encontrados.add(filas.getString("id"));
                }
            }
            if (!encontrados.equals(idsRecursos)) {
                Set<String> faltantes = new LinkedHashSet<>(idsRecursos);
                faltantes.removeAll(encontrados);
                throw new ConflictoException(
                        "No existen los recursos: " + String.join(", ", faltantes) + ".");
            }
        }
    }

    private static Set<String> recursosOcupados(Connection conexion, LocalDate fecha,
                                                LocalTime horaInicio, LocalTime horaFin,
                                                Set<String> idsRecursos) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT rr.recurso_id
                FROM reserva_recursos rr
                JOIN reservas r ON r.id = rr.reserva_id
                WHERE r.estado = 'ACTIVA'
                  AND r.fecha = ?
                  AND r.hora_inicio < ?
                  AND r.hora_fin > ?
                """);
        if (idsRecursos != null) {
            sql.append("  AND rr.recurso_id IN (")
                    .append(String.join(", ",
                            java.util.Collections.nCopies(idsRecursos.size(), "?")))
                    .append(")\n");
        }
        try (PreparedStatement sentencia = conexion.prepareStatement(sql.toString())) {
            sentencia.setDate(1, Date.valueOf(fecha));
            sentencia.setTime(2, Time.valueOf(horaFin));
            sentencia.setTime(3, Time.valueOf(horaInicio));
            int indice = 4;
            if (idsRecursos != null) {
                for (String id : idsRecursos) {
                    sentencia.setString(indice++, id);
                }
            }
            Set<String> ocupados = new LinkedHashSet<>();
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    ocupados.add(filas.getString("recurso_id"));
                }
            }
            return ocupados;
        }
    }

    private static String describirRecursos(Reserva reserva, Set<String> idsOcupados) {
        List<String> descripciones = reserva.getRecursos().stream()
                .filter(recurso -> idsOcupados.contains(recurso.getId()))
                .map(Recurso::getDescripcion)
                .toList();
        return String.join(", ", descripciones);
    }

    private static FilaReserva mapearFila(ResultSet filas) throws SQLException {
        Funcionario funcionario = new Funcionario(
                filas.getString("funcionario_id"),
                filas.getString("funcionario_nombre"),
                filas.getString("funcionario_telefono"));
        return new FilaReserva(
                filas.getString("id"),
                funcionario,
                filas.getString("actividad"),
                filas.getDate("fecha").toLocalDate(),
                filas.getTime("hora_inicio").toLocalTime(),
                filas.getTime("hora_fin").toLocalTime(),
                EstadoReserva.valueOf(filas.getString("estado")));
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ValidacionException("Debe indicar la fecha desde y la fecha hasta.");
        }
        if (desde.isAfter(hasta)) {
            throw new ValidacionException(
                    "La fecha desde no puede ser posterior a la fecha hasta.");
        }
    }

    private static void validarHorario(LocalTime horaInicio, LocalTime horaFin) {
        if (horaInicio == null || horaFin == null) {
            throw new ValidacionException("Debe indicar la hora de inicio y la hora de fin.");
        }
        if (!horaFin.isAfter(horaInicio)) {
            throw new ValidacionException(
                    "La hora de fin debe ser posterior a la hora de inicio.");
        }
    }

    private record FilaReserva(String id, Funcionario funcionario, String actividad,
                               LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                               EstadoReserva estado) {
    }
}
