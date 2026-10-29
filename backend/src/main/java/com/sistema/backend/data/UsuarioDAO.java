package com.sistema.backend.data;

import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Administrador;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Rol;
import com.sistema.common.modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Acceso a la tabla {@code usuarios} (administradores y funcionarios).
 *
 * <p>La clave nunca se devuelve al modelo: solo se expone
 * {@link #buscarClaveHash(String)} para que la capa lógica verifique el hash
 * BCrypt durante el inicio de sesión.</p>
 */
public final class UsuarioDAO extends DAOBase {

    private static final String COLUMNAS = "id, clave_hash, rol, nombre, telefono";
    private static final Pattern HASH_BCRYPT =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    public UsuarioDAO(FuenteDatos fuente) {
        super(fuente);
    }

    public List<Usuario> listarTodos() {
        return enConsulta("listar usuarios", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM usuarios ORDER BY id");
                 ResultSet filas = sentencia.executeQuery()) {
                List<Usuario> usuarios = new ArrayList<>();
                while (filas.next()) {
                    usuarios.add(mapear(filas));
                }
                return usuarios;
            }
        });
    }

    public List<Funcionario> listarFuncionarios() {
        return enConsulta("listar funcionarios", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM usuarios WHERE rol = 'FUNCIONARIO' "
                            + "ORDER BY nombre, id");
                 ResultSet filas = sentencia.executeQuery()) {
                List<Funcionario> funcionarios = new ArrayList<>();
                while (filas.next()) {
                    funcionarios.add((Funcionario) mapear(filas));
                }
                return funcionarios;
            }
        });
    }

    /**
     * Busca funcionarios por identificador, nombre o teléfono. Un criterio
     * vacío devuelve todos.
     */
    public List<Funcionario> buscarFuncionarios(String criterio) {
        String patron = "%" + (criterio == null ? "" : criterio.trim()) + "%";
        return enConsulta("buscar funcionarios", conexion -> {
            String sql = "SELECT " + COLUMNAS + " FROM usuarios WHERE rol = 'FUNCIONARIO' "
                    + "AND (id LIKE ? OR nombre LIKE ? OR telefono LIKE ?) ORDER BY nombre, id";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setString(1, patron);
                sentencia.setString(2, patron);
                sentencia.setString(3, patron);
                try (ResultSet filas = sentencia.executeQuery()) {
                    List<Funcionario> funcionarios = new ArrayList<>();
                    while (filas.next()) {
                        funcionarios.add((Funcionario) mapear(filas));
                    }
                    return funcionarios;
                }
            }
        });
    }

    public Optional<Usuario> buscarPorId(String id) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        return enConsulta("buscar usuario", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM usuarios WHERE id = ?")) {
                sentencia.setString(1, id);
                try (ResultSet filas = sentencia.executeQuery()) {
                    return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
                }
            }
        });
    }

    /**
     * Devuelve el hash BCrypt del usuario, necesario para validar el login.
     */
    public Optional<String> buscarClaveHash(String id) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        return enConsulta("buscar clave de usuario", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT clave_hash FROM usuarios WHERE id = ?")) {
                sentencia.setString(1, id);
                try (ResultSet filas = sentencia.executeQuery()) {
                    return filas.next() ? Optional.of(filas.getString("clave_hash")) : Optional.empty();
                }
            }
        });
    }

    public boolean existeId(String id) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        return enConsulta("verificar usuario", conexion -> existe(conexion, id));
    }

    /**
     * Inserta un usuario con su clave ya hasheada.
     *
     * @param usuario usuario a insertar
     * @param claveHash hash BCrypt de la clave; nunca la clave en texto plano
     */
    public void insertar(Usuario usuario, String claveHash) {
        Objects.requireNonNull(usuario, "El usuario es obligatorio.");
        validarHash(claveHash);
        String nombre = usuario instanceof Funcionario funcionario ? funcionario.getNombre() : null;
        String telefono = usuario instanceof Funcionario funcionario ? funcionario.getTelefono() : null;
        enTransaccion("insertar usuario", conexion -> {
            String sql = "INSERT INTO usuarios (id, clave_hash, rol, nombre, telefono) "
                    + "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setString(1, usuario.getId());
                sentencia.setString(2, claveHash);
                sentencia.setString(3, usuario.getRol().name());
                sentencia.setString(4, nombre);
                sentencia.setString(5, telefono);
                sentencia.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Actualiza el nombre y el teléfono de un funcionario existente.
     */
    public void actualizarFuncionario(Funcionario funcionario) {
        Objects.requireNonNull(funcionario, "El funcionario es obligatorio.");
        enTransaccion("actualizar funcionario", conexion -> {
            String sql = "UPDATE usuarios SET nombre = ?, telefono = ? "
                    + "WHERE id = ? AND rol = 'FUNCIONARIO'";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                sentencia.setString(1, funcionario.getNombre());
                sentencia.setString(2, funcionario.getTelefono());
                sentencia.setString(3, funcionario.getId());
                if (sentencia.executeUpdate() == 0 && !existe(conexion, funcionario.getId())) {
                    throw new NoEncontradoException(
                            "No existe el funcionario con identificador " + funcionario.getId() + ".");
                }
            }
            return null;
        });
    }

    /**
     * Reemplaza la clave por un nuevo hash BCrypt.
     */
    public void actualizarClave(String id, String nuevaClaveHash) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        validarHash(nuevaClaveHash);
        enTransaccion("actualizar clave", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "UPDATE usuarios SET clave_hash = ? WHERE id = ?")) {
                sentencia.setString(1, nuevaClaveHash);
                sentencia.setString(2, id);
                if (sentencia.executeUpdate() == 0) {
                    throw new NoEncontradoException(
                            "No existe el usuario con identificador " + id + ".");
                }
            }
            return null;
        });
    }

    public void eliminar(String id) {
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        enTransaccion("eliminar usuario", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "DELETE FROM usuarios WHERE id = ?")) {
                sentencia.setString(1, id);
                if (sentencia.executeUpdate() == 0) {
                    throw new NoEncontradoException(
                            "No existe el usuario con identificador " + id + ".");
                }
            }
            return null;
        });
    }

    public long contarFuncionarios() {
        return enConsulta("contar funcionarios", conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT COUNT(*) FROM usuarios WHERE rol = 'FUNCIONARIO'");
                 ResultSet filas = sentencia.executeQuery()) {
                filas.next();
                return filas.getLong(1);
            }
        });
    }

    private static boolean existe(Connection conexion, String id) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT 1 FROM usuarios WHERE id = ?")) {
            sentencia.setString(1, id);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next();
            }
        }
    }

    private static Usuario mapear(ResultSet filas) throws SQLException {
        String id = filas.getString("id");
        Rol rol = Rol.valueOf(filas.getString("rol"));
        if (rol == Rol.ADMINISTRADOR) {
            return new Administrador(id);
        }
        return new Funcionario(id, filas.getString("nombre"), filas.getString("telefono"));
    }

    private static void validarHash(String claveHash) {
        if (claveHash == null || !HASH_BCRYPT.matcher(claveHash).matches()) {
            throw new ValidacionException(
                    "La clave debe guardarse como un hash BCrypt válido, nunca en texto plano.");
        }
    }
}
