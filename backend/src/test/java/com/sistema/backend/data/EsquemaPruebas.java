package com.sistema.backend.data;

import com.sistema.backend.config.ConfiguracionBackend;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Crea la base de pruebas {@code proyectoII_reservas_test} a partir del mismo
 * {@code sql/schema.sql} que se usa en desarrollo.
 *
 * <p>El POM del backend copia {@code sql/schema.sql} a los recursos de prueba,
 * de modo que las pruebas nunca quedan desincronizadas del esquema real. Aquí
 * solo se cambia el nombre de la base.</p>
 */
final class EsquemaPruebas {

    static final String BASE_PRUEBAS = "proyectoII_reservas_test";
    private static final String BASE_PRODUCCION = "proyectoII_reservas";

    private EsquemaPruebas() {
    }

    static void recrear(ConfiguracionBackend configuracion) throws SQLException {
        String script = leerRecurso("/schema.sql")
                .replace(BASE_PRODUCCION, BASE_PRUEBAS);
        List<String> sentencias = dividir(script);
        String urlServidor = urlSinBase(configuracion.urlBd());
        try (Connection conexion = DriverManager.getConnection(
                urlServidor, configuracion.usuarioBd(), configuracion.claveBd());
             Statement sentencia = conexion.createStatement()) {
            for (String sql : sentencias) {
                sentencia.execute(sql);
            }
        }
    }

    private static String leerRecurso(String ruta) {
        try (InputStream entrada = EsquemaPruebas.class.getResourceAsStream(ruta)) {
            if (entrada == null) {
                throw new IllegalStateException(
                        "No se encontró " + ruta + " en los recursos de prueba. "
                                + "Verifique la copia de sql/schema.sql en backend/pom.xml.");
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + ruta + ".", e);
        }
    }

    /**
     * Quita los comentarios de línea y separa por punto y coma. El esquema no
     * usa procedimientos ni disparadores, así que esta división es suficiente.
     */
    private static List<String> dividir(String script) {
        String sinComentarios = script.lines()
                .map(String::strip)
                .filter(linea -> !linea.isEmpty() && !linea.startsWith("--"))
                .collect(Collectors.joining("\n"));
        return Arrays.stream(sinComentarios.split(";"))
                .map(String::strip)
                .filter(sentencia -> !sentencia.isEmpty())
                .toList();
    }

    /**
     * Convierte {@code jdbc:mysql://host:puerto/base?opciones} en
     * {@code jdbc:mysql://host:puerto/?opciones} para poder crear la base.
     */
    static String urlSinBase(String url) {
        int inicio = url.indexOf("://");
        int barra = url.indexOf('/', inicio + 3);
        int interrogacion = url.indexOf('?', inicio + 3);
        boolean hayBase = barra >= 0 && (interrogacion < 0 || barra < interrogacion);
        if (!hayBase) {
            String base = interrogacion >= 0 ? url.substring(0, interrogacion) : url;
            String opciones = interrogacion >= 0 ? url.substring(interrogacion) : "";
            return base + "/" + opciones;
        }
        String opciones = interrogacion >= 0 ? url.substring(interrogacion) : "";
        return url.substring(0, barra + 1) + opciones;
    }
}
