package com.sistema.backend.data;

import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Recurso;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Acceso a la tabla {@code recursos}, siempre junto con su categoría.
 */
public final class RecursoDAO extends DAOBase {

    private static final String SELECT_BASE = """
            SELECT r.id, r.descripcion, c.id AS categoria_id,
                   c.descripcion AS categoria_descripcion
            FROM recursos r
            JOIN categorias c ON c.id = r.categoria_id
            """;

    public RecursoDAO(FuenteDatos fuente) {
        super(fuente);
    }

    public List<Recurso> listarTodos() {
        return consultar(SELECT_BASE + " ORDER BY r.id");
    }

    public List<Recurso> listarPorCategoria(String categoriaId) {
        Objects.requireNonNull(categoriaId, "La categoría es obligatoria.");
        return enConsulta("listar recursos de la categoría", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    SELECT_BASE + " WHERE r.categoria_id = ? ORDER BY r.id")) {
                sentencia.setString(1, categoriaId);
                try (ResultSet filas = sentencia.executeQuery()) {
                    return mapearTodos(filas);
                }
            }
        });
    }

    /**
     * Busca por identificador, descripción o nombre de la categoría.
     * Un criterio vacío devuelve todos.
     */
    public List<Recurso> buscar(String criterio) {
        String patron = "%" + (criterio == null ? "" : criterio.trim()) + "%";
        return enConsulta("buscar recursos", conexion -> {
            String sql = SELECT_BASE
                    + " WHERE r.id LIKE ? OR r.descripcion LIKE ? OR c.descripcion LIKE ?"
                    + " ORDER BY r.id";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setString(1, patron);
                sentencia.setString(2, patron);
                sentencia.setString(3, patron);
                try (ResultSet filas = sentencia.executeQuery()) {
                    return mapearTodos(filas);
                }
            }
        });
    }

    /**
     * Filtra por categoría (opcional) y criterio de texto (opcional).
     */
    public List<Recurso> filtrar(String categoriaId, String criterio) {
        String patron = "%" + (criterio == null ? "" : criterio.trim()) + "%";
        boolean conCategoria = categoriaId != null && !categoriaId.isBlank();
        return enConsulta("filtrar recursos", conexion -> {
            String sql = SELECT_BASE
                    + " WHERE (? = FALSE OR r.categoria_id = ?)"
                    + " AND (r.id LIKE ? OR r.descripcion LIKE ? OR c.descripcion LIKE ?)"
                    + " ORDER BY r.id";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setBoolean(1, conCategoria);
                sentencia.setString(2, conCategoria ? categoriaId : "");
                sentencia.setString(3, patron);
                sentencia.setString(4, patron);
                sentencia.setString(5, patron);
                try (ResultSet filas = sentencia.executeQuery()) {
                    return mapearTodos(filas);
                }
            }
        });
    }

    public Optional<Recurso> buscarPorId(String id) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        return enConsulta("buscar recurso", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    SELECT_BASE + " WHERE r.id = ?")) {
                sentencia.setString(1, id);
                try (ResultSet filas = sentencia.executeQuery()) {
                    return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
                }
            }
        });
    }

    public boolean existeId(String id) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        return enConsulta("verificar recurso", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT 1 FROM recursos WHERE id = ?")) {
                sentencia.setString(1, id);
                try (ResultSet filas = sentencia.executeQuery()) {
                    return filas.next();
                }
            }
        });
    }

    /**
     * Indica si otra descripción de la misma categoría ya existe.
     *
     * @param categoriaId categoría donde se busca
     * @param descripcion descripción a comprobar
     * @param excluirId   recurso a excluir (al modificar), o {@code null}
     */
    public boolean existeDescripcionEnCategoria(String categoriaId, String descripcion,
                                                String excluirId) {
        Objects.requireNonNull(categoriaId, "La categoría es obligatoria.");
        Objects.requireNonNull(descripcion, "La descripción es obligatoria.");
        return enConsulta("verificar descripción de recurso", conexion -> {
            String sql = excluirId == null
                    ? "SELECT 1 FROM recursos WHERE categoria_id = ? AND descripcion = ?"
                    : "SELECT 1 FROM recursos WHERE categoria_id = ? AND descripcion = ? "
                            + "AND id <> ?";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setString(1, categoriaId);
                sentencia.setString(2, descripcion.trim());
                if (excluirId != null) {
                    sentencia.setString(3, excluirId);
                }
                try (ResultSet filas = sentencia.executeQuery()) {
                    return filas.next();
                }
            }
        });
    }

    public void insertar(Recurso recurso) {
        Objects.requireNonNull(recurso, "El recurso es obligatorio.");
        enTransaccion("insertar recurso", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO recursos (id, categoria_id, descripcion) VALUES (?, ?, ?)")) {
                sentencia.setString(1, recurso.getId());
                sentencia.setString(2, recurso.getCategoria().getId());
                sentencia.setString(3, recurso.getDescripcion());
                sentencia.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Actualiza identificador, categoría y descripción. El identificador puede
     * cambiar: {@code reserva_recursos} usa {@code ON UPDATE CASCADE}.
     */
    public void actualizar(String idOriginal, Recurso recurso) {
        Objects.requireNonNull(idOriginal, "El identificador original es obligatorio.");
        Objects.requireNonNull(recurso, "El recurso es obligatorio.");
        enTransaccion("actualizar recurso", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "UPDATE recursos SET id = ?, categoria_id = ?, descripcion = ? WHERE id = ?")) {
                sentencia.setString(1, recurso.getId());
                sentencia.setString(2, recurso.getCategoria().getId());
                sentencia.setString(3, recurso.getDescripcion());
                sentencia.setString(4, idOriginal);
                if (sentencia.executeUpdate() == 0 && !existe(conexion, idOriginal)) {
                    throw new NoEncontradoException(
                            "No existe el recurso con identificador " + idOriginal + ".");
                }
            }
            return null;
        });
    }

    public void eliminar(String id) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        enTransaccion("eliminar recurso", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "DELETE FROM recursos WHERE id = ?")) {
                sentencia.setString(1, id);
                if (sentencia.executeUpdate() == 0) {
                    throw new NoEncontradoException(
                            "No existe el recurso con identificador " + id + ".");
                }
            }
            return null;
        });
    }

    private List<Recurso> consultar(String sql) {
        return enConsulta("listar recursos", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(sql);
                 ResultSet filas = sentencia.executeQuery()) {
                return mapearTodos(filas);
            }
        });
    }

    private static boolean existe(Connection conexion, String id) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT 1 FROM recursos WHERE id = ?")) {
            sentencia.setString(1, id);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next();
            }
        }
    }

    private static List<Recurso> mapearTodos(ResultSet filas) throws SQLException {
        List<Recurso> recursos = new ArrayList<>();
        while (filas.next()) {
            recursos.add(mapear(filas));
        }
        return recursos;
    }

    private static Recurso mapear(ResultSet filas) throws SQLException {
        CategoriaRecurso categoria = new CategoriaRecurso(
                filas.getString("categoria_id"),
                filas.getString("categoria_descripcion"));
        return new Recurso(filas.getString("id"), categoria, filas.getString("descripcion"));
    }
}
