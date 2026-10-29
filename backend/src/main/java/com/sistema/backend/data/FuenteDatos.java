package com.sistema.backend.data;

import com.sistema.backend.config.ConfiguracionBackend;
import com.sistema.common.excepciones.AccesoDatosException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Pool de conexiones a MySQL basado en <strong>HikariCP</strong>.
 *
 * <p>Se eligió HikariCP en lugar de un pool propio porque es la implementación
 * recomendada para JDBC: reutiliza conexiones, valida que sigan vivas, limita
 * el número máximo de conexiones y evita el costo de abrir una conexión nueva
 * en cada operación. Es una dependencia pequeña y de configuración mínima.</p>
 *
 * <p>Todos los DAOs reciben una instancia de esta clase; así el backend es el
 * único módulo que conoce el driver de MySQL.</p>
 */
public final class FuenteDatos implements AutoCloseable {

    private static final int TIEMPO_ESPERA_MS = 10_000;

    private final HikariDataSource pool;

    public FuenteDatos(ConfiguracionBackend configuracion) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("sistema-reservas");
        hikari.setJdbcUrl(configuracion.urlBd());
        hikari.setUsername(configuracion.usuarioBd());
        hikari.setPassword(configuracion.claveBd());
        hikari.setMaximumPoolSize(configuracion.tamanoPool());
        hikari.setMinimumIdle(Math.min(2, configuracion.tamanoPool()));
        hikari.setConnectionTimeout(TIEMPO_ESPERA_MS);
        hikari.setValidationTimeout(5_000);
        hikari.setConnectionTestQuery("SELECT 1");
        this.pool = new HikariDataSource(hikari);
    }

    /**
     * Obtiene una conexión del pool. Quien la recibe debe cerrarla
     * (preferiblemente con {@code try-with-resources}); cerrarla la devuelve
     * al pool, no la destruye.
     */
    public Connection conectar() {
        try {
            return pool.getConnection();
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "No se pudo obtener una conexión a la base de datos. "
                            + "Verifique que MySQL esté activo y que backend.properties sea correcto.", e);
        }
    }

    @Override
    public void close() {
        pool.close();
    }
}
