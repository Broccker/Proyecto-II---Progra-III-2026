package com.sistema.backend.data;

import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.modelo.CategoriaRecurso;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Acceso a la tabla {@code categorias}.
 */
public final class CategoriaRecursoDAO extends DAOBase {

    private static final String PREFIJO_ID = "CAT-";

    public CategoriaRecursoDAO(FuenteDatos fuente) {
        super(fuente);
    }

    public List<CategoriaRecurso> listarTodas() {
        return enConsulta("listar categorías", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, descripcion FROM categorias ORDER BY id");
                 ResultSet filas = sentencia.executeQuery()) {
                List<CategoriaRecurso> categorias = new ArrayList<>();
                while (filas.next()) {
                    categorias.add(mapear(filas));
                }
                return categorias;
            }
        });
    }

    /**
     * Busca por identificador o descripción. Un criterio vacío devuelve todas.
     */
    public List<CategoriaRecurso> buscar(String criterio) {
        String patron = "%" + (criterio == null ? "" : criterio.trim()) + "%";
        return enConsulta("buscar categorías", conexion -> {
            String sql = "SELECT id, descripcion FROM categorias "
                    + "WHERE id LIKE ? OR descripcion LIKE ? ORDER BY id";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setString(1, patron);
                sentencia.setString(2, patron);
                try (ResultSet filas = sentencia.executeQuery()) {
                    List<CategoriaRecurso> categorias = new ArrayList<>();
                    while (filas.next()) {
                        categorias.add(mapear(filas));
                    }
                    return categorias;
                }
            }
        });
    }

    public Optional<CategoriaRecurso> buscarPorId(String id) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        return enConsulta("buscar categoría", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, descripcion FROM categorias WHERE id = ?")) {
                sentencia.setString(1, id);
                try (ResultSet filas = sentencia.executeQuery()) {
                    return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
                }
            }
        });
    }

    public Optional<CategoriaRecurso> buscarPorDescripcion(String descripcion) {
        Objects.requireNonNull(descripcion, "La descripción es obligatoria.");
        return enConsulta("buscar categoría por descripción", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, descripcion FROM categorias WHERE descripcion = ?")) {
                sentencia.setString(1, descripcion.trim());
                try (ResultSet filas = sentencia.executeQuery()) {
                    return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
                }
            }
        });
    }

    public boolean existeId(String id) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        return enConsulta("verificar categoría", conexion -> existe(conexion, id));
    }

    public boolean existeDescripcion(String descripcion) {
        return existeDescripcion(descripcion, null);
    }

    /**
     * Indica si otra categoría (distinta de {@code excluirId}) ya usa la
     * descripción. Se usa al modificar para no chocar consigo misma.
     */
    public boolean existeDescripcion(String descripcion, String excluirId) {
        Objects.requireNonNull(descripcion, "La descripción es obligatoria.");
        return enConsulta("verificar descripción de categoría", conexion -> {
            String sql = excluirId == null
                    ? "SELECT 1 FROM categorias WHERE descripcion = ?"
                    : "SELECT 1 FROM categorias WHERE descripcion = ? AND id <> ?";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setString(1, descripcion.trim());
                if (excluirId != null) {
                    sentencia.setString(2, excluirId);
                }
                try (ResultSet filas = sentencia.executeQuery()) {
                    return filas.next();
                }
            }
        });
    }

    /**
     * Calcula el siguiente identificador con el formato {@code CAT-000001}.
     */
    public String siguienteId() {
        return enConsulta("calcular siguiente categoría", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT MAX(id) FROM categorias");
                 ResultSet filas = sentencia.executeQuery()) {
                filas.next();
                String maximo = filas.getString(1);
                int numero = 0;
                if (maximo != null && maximo.startsWith(PREFIJO_ID)) {
                    numero = Integer.parseInt(maximo.substring(PREFIJO_ID.length()));
                }
                return String.format("%s%06d", PREFIJO_ID, numero + 1);
            }
        });
    }

    public void insertar(CategoriaRecurso categoria) {
        Objects.requireNonNull(categoria, "La categoría es obligatoria.");
        enTransaccion("insertar categoría", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO categorias (id, descripcion) VALUES (?, ?)")) {
                sentencia.setString(1, categoria.getId());
                sentencia.setString(2, categoria.getDescripcion());
                sentencia.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Actualiza identificador y descripción. El identificador puede cambiar:
     * la clave foránea de {@code recursos} usa {@code ON UPDATE CASCADE}.
     */
    public void actualizar(String idOriginal, CategoriaRecurso categoria) {
        Objects.requireNonNull(idOriginal, "El identificador original es obligatorio.");
        Objects.requireNonNull(categoria, "La categoría es obligatoria.");
        enTransaccion("actualizar categoría", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "UPDATE categorias SET id = ?, descripcion = ? WHERE id = ?")) {
                sentencia.setString(1, categoria.getId());
                sentencia.setString(2, categoria.getDescripcion());
                sentencia.setString(3, idOriginal);
                if (sentencia.executeUpdate() == 0 && !existe(conexion, idOriginal)) {
                    throw new NoEncontradoException(
                            "No existe la categoría con identificador " + idOriginal + ".");
                }
            }
            return null;
        });
    }

    public void eliminar(String id) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        enTransaccion("eliminar categoría", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "DELETE FROM categorias WHERE id = ?")) {
                sentencia.setString(1, id);
                if (sentencia.executeUpdate() == 0) {
                    throw new NoEncontradoException(
                            "No existe la categoría con identificador " + id + ".");
                }
            }
            return null;
        });
    }

    public long contarRecursos(String categoriaId) {
        Objects.requireNonNull(categoriaId, "El identificador es obligatorio.");
        return enConsulta("contar recursos de la categoría", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT COUNT(*) FROM recursos WHERE categoria_id = ?")) {
                sentencia.setString(1, categoriaId);
                try (ResultSet filas = sentencia.executeQuery()) {
                    filas.next();
                    return filas.getLong(1);
                }
            }
        });
    }

    private static boolean existe(Connection conexion, String id) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT 1 FROM categorias WHERE id = ?")) {
            sentencia.setString(1, id);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next();
            }
        }
    }

    private static CategoriaRecurso mapear(ResultSet filas) throws SQLException {
        return new CategoriaRecurso(filas.getString("id"), filas.getString("descripcion"));
    }
}
