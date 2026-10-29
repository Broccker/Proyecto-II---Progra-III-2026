package com.sistema.backend.data;

import com.sistema.backend.config.ConfiguracionBackend;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.EstadoReserva;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Base de las pruebas de integración de los DAO.
 *
 * <p>Al arrancar la JVM recrea la base {@code proyectoII_reservas_test} con el
 * esquema real y abre un pool HikariCP pequeño. Antes de cada prueba deja las
 * tablas vacías para que los resultados sean independientes.</p>
 *
 * <p>Si MySQL no está disponible, las pruebas se marcan como omitidas con un
 * mensaje claro en el reporte de Failsafe, en lugar de romper la compilación
 * de un compañero que solo está trabajando en el frontend.</p>
 */
abstract class PruebaBDBase {

    /** Clave de prueba hasheada una sola vez para todas las pruebas. */
    protected static final String CLAVE_HASH = BCrypt.hashpw("clave", BCrypt.gensalt(4));

    private static final Set<String> TABLAS = Set.of(
            "usuarios", "categorias", "recursos", "reservas", "reserva_recursos");

    private static final FuenteDatos FUENTE;
    private static final String MOTIVO_OMISION;

    static {
        FuenteDatos fuente = null;
        String motivo = null;
        try {
            ConfiguracionBackend configuracion =
                    ConfiguracionBackend.desdeClasspath("/backend-test.properties");
            EsquemaPruebas.recrear(configuracion);
            fuente = new FuenteDatos(configuracion);
            FuenteDatos pool = fuente;
            Runtime.getRuntime().addShutdownHook(
                    new Thread(pool::close, "cierre-pool-pruebas"));
        } catch (RuntimeException | SQLException e) {
            motivo = "Base de pruebas no disponible (" + e.getMessage() + "). "
                    + "Inicie MySQL y ejecute de nuevo 'mvn verify'.";
        }
        FUENTE = fuente;
        MOTIVO_OMISION = motivo;
    }

    protected FuenteDatos fuente() {
        Assumptions.assumeTrue(FUENTE != null, MOTIVO_OMISION);
        return FUENTE;
    }

    @BeforeEach
    void limpiarTablas() throws SQLException {
        try (Connection conexion = fuente().conectar();
             Statement sentencia = conexion.createStatement()) {
            sentencia.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String tabla : List.of("reserva_recursos", "reservas", "recursos",
                    "categorias", "usuarios")) {
                sentencia.execute("DELETE FROM " + tabla);
            }
            sentencia.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    protected Funcionario crearFuncionario(String id, String nombre, String telefono) {
        Funcionario funcionario = new Funcionario(id, nombre, telefono);
        new UsuarioDAO(fuente()).insertar(funcionario, CLAVE_HASH);
        return funcionario;
    }

    protected CategoriaRecurso crearCategoria(String id, String descripcion) {
        CategoriaRecurso categoria = new CategoriaRecurso(id, descripcion);
        new CategoriaRecursoDAO(fuente()).insertar(categoria);
        return categoria;
    }

    protected Recurso crearRecurso(String id, CategoriaRecurso categoria, String descripcion) {
        Recurso recurso = new Recurso(id, categoria, descripcion);
        new RecursoDAO(fuente()).insertar(recurso);
        return recurso;
    }

    protected Reserva insertarReserva(String id, Funcionario funcionario, LocalDate fecha,
                                      String horaInicio, String horaFin, Recurso... recursos) {
        Reserva reserva = new Reserva(id, funcionario, "Actividad " + id, fecha,
                LocalTime.parse(horaInicio), LocalTime.parse(horaFin),
                List.of(recursos), EstadoReserva.ACTIVA);
        new ReservaDAO(fuente()).insertar(reserva);
        return reserva;
    }

    /**
     * Ejecuta una sentencia de apoyo (por ejemplo, para preparar un caso de
     * clave foránea). No se usa para probar los DAO.
     */
    protected int ejecutar(String sql, Object... parametros) {
        try (Connection conexion = fuente().conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                sentencia.setObject(i + 1, parametros[i]);
            }
            return sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo ejecutar el SQL de apoyo.", e);
        }
    }

    protected long contar(String tabla) {
        if (!TABLAS.contains(tabla)) {
            throw new IllegalArgumentException("Tabla no permitida: " + tabla);
        }
        try (Connection conexion = fuente().conectar();
             PreparedStatement sentencia = conexion.prepareStatement(
                     "SELECT COUNT(*) FROM " + tabla);
             ResultSet filas = sentencia.executeQuery()) {
            filas.next();
            return filas.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo contar la tabla " + tabla + ".", e);
        }
    }
}
