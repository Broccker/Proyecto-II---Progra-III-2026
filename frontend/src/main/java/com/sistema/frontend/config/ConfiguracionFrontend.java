package com.sistema.frontend.config;

import com.sistema.common.excepciones.ConfiguracionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * Configuración del frontend: a qué backend conectarse y cuánto esperar.
 *
 * <p>Orden de búsqueda al usar {@link #cargar()}:</p>
 * <ol>
 *   <li>La ruta indicada con {@code -Dfrontend.config=/ruta/frontend.properties}.</li>
 *   <li>La variable de entorno {@code FRONTEND_CONFIG}.</li>
 *   <li>El recurso {@code /frontend.properties} del classpath.</li>
 *   <li>Valores por defecto ({@code 127.0.0.1:5000}, 15 s).</li>
 * </ol>
 *
 * <p>Las propiedades del sistema {@code servidor.host},
 * {@code servidor.puerto} y {@code proxy.timeout.ms} sobrescriben el archivo.
 * El archivo es opcional: el frontend solo guarda el destino, nunca
 * credenciales de base de datos ni claves de servicios externos.</p>
 */
public record ConfiguracionFrontend(String host, int puerto, int tiempoEsperaMs) {

    /** Recurso del classpath con la configuración opcional. */
    public static final String RECURSO_POR_DEFECTO = "/frontend.properties";

    private static final String HOST_POR_DEFECTO = "127.0.0.1";
    private static final int PUERTO_POR_DEFECTO = 5000;
    private static final int ESPERA_POR_DEFECTO_MS = 15_000;
    private static final int ESPERA_MINIMA_MS = 100;
    private static final int ESPERA_MAXIMA_MS = 300_000;

    public ConfiguracionFrontend {
        if (host == null || host.isBlank()) {
            throw new ConfiguracionException(
                    "Falta el host del backend (servidor.host).");
        }
        host = host.trim();
        if (puerto < 1 || puerto > 65535) {
            throw new ConfiguracionException(
                    "El puerto del backend debe estar entre 1 y 65535 (valor actual: "
                            + puerto + ").");
        }
        if (tiempoEsperaMs < ESPERA_MINIMA_MS || tiempoEsperaMs > ESPERA_MAXIMA_MS) {
            throw new ConfiguracionException("El tiempo de espera del proxy debe estar entre "
                    + ESPERA_MINIMA_MS + " y " + ESPERA_MAXIMA_MS + " ms (valor actual: "
                    + tiempoEsperaMs + ").");
        }
    }

    /**
     * Carga la configuración con el orden de búsqueda documentado en la clase.
     */
    public static ConfiguracionFrontend cargar() {
        String rutaExterna = System.getProperty("frontend.config");
        if (rutaExterna == null || rutaExterna.isBlank()) {
            rutaExterna = System.getenv("FRONTEND_CONFIG");
        }
        if (rutaExterna != null && !rutaExterna.isBlank()) {
            return desdeArchivo(Path.of(rutaExterna));
        }
        if (ConfiguracionFrontend.class.getResource(RECURSO_POR_DEFECTO) == null) {
            return porDefecto();
        }
        return desdeClasspath(RECURSO_POR_DEFECTO);
    }

    /**
     * Configuración por defecto: backend local en el puerto 5000.
     */
    public static ConfiguracionFrontend porDefecto() {
        return new ConfiguracionFrontend(HOST_POR_DEFECTO, PUERTO_POR_DEFECTO,
                ESPERA_POR_DEFECTO_MS);
    }

    /**
     * Carga un archivo de configuración concreto del classpath.
     * Pensado para las pruebas ({@code /frontend-test.properties}).
     */
    public static ConfiguracionFrontend desdeClasspath(String recurso) {
        try (InputStream entrada = ConfiguracionFrontend.class.getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new ConfiguracionException(
                        "No se encontró '" + recurso + "'. Copie frontend.properties.example "
                                + "a frontend.properties si necesita cambiar el destino.");
            }
            try (Reader lector = new InputStreamReader(entrada, StandardCharsets.UTF_8)) {
                Properties propiedades = new Properties();
                propiedades.load(lector);
                return desdePropiedades(propiedades);
            }
        } catch (IOException e) {
            throw new ConfiguracionException("No se pudo leer el archivo " + recurso + ".", e);
        }
    }

    /**
     * Carga un archivo de configuración del sistema de archivos.
     */
    public static ConfiguracionFrontend desdeArchivo(Path archivo) {
        Objects.requireNonNull(archivo, "La ruta del archivo de configuración es obligatoria.");
        if (!Files.isRegularFile(archivo)) {
            throw new ConfiguracionException("No existe el archivo de configuración: " + archivo);
        }
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            Properties propiedades = new Properties();
            propiedades.load(lector);
            return desdePropiedades(propiedades);
        } catch (IOException e) {
            throw new ConfiguracionException("No se pudo leer el archivo " + archivo + ".", e);
        }
    }

    /**
     * Devuelve una configuración equivalente apuntando a otro destino.
     * Se usa en las pruebas y en los argumentos de {@code ClienteMain}.
     */
    public ConfiguracionFrontend conDestino(String nuevoHost, int nuevoPuerto) {
        return new ConfiguracionFrontend(nuevoHost, nuevoPuerto, tiempoEsperaMs);
    }

    /**
     * Aplica argumentos posicionales {@code [host] [puerto]} sobre esta
     * configuración. Sin argumentos se conserva tal cual.
     */
    public ConfiguracionFrontend conArgumentos(String[] argumentos) {
        if (argumentos == null || argumentos.length == 0) {
            return this;
        }
        if (argumentos.length > 2) {
            throw new ConfiguracionException(
                    "Uso: ClienteMain [host] [puerto]. Se recibieron "
                            + argumentos.length + " argumentos.");
        }
        String nuevoHost = argumentos[0] == null || argumentos[0].isBlank()
                ? host : argumentos[0].trim();
        int nuevoPuerto = argumentos.length == 2
                ? entero(argumentos[1], "servidor.puerto") : puerto;
        return new ConfiguracionFrontend(nuevoHost, nuevoPuerto, tiempoEsperaMs);
    }

    private static ConfiguracionFrontend desdePropiedades(Properties propiedades) {
        String host = valor(propiedades, "servidor.host");
        int puerto = entero(valor(propiedades, "servidor.puerto"), "servidor.puerto",
                PUERTO_POR_DEFECTO);
        int espera = entero(valor(propiedades, "proxy.timeout.ms"), "proxy.timeout.ms",
                ESPERA_POR_DEFECTO_MS);
        return new ConfiguracionFrontend(host == null ? HOST_POR_DEFECTO : host, puerto, espera);
    }

    private static String valor(Properties propiedades, String clave) {
        String delSistema = System.getProperty(clave);
        String valor = delSistema != null ? delSistema : propiedades.getProperty(clave);
        return valor == null || valor.isBlank() ? null : valor;
    }

    private static int entero(String valor, String clave, int porDefecto) {
        if (valor == null) {
            return porDefecto;
        }
        return entero(valor, clave);
    }

    private static int entero(String valor, String clave) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new ConfiguracionException(
                    "La propiedad '" + clave + "' debe ser un número entero (valor actual: "
                            + valor + ").", e);
        }
    }
}
