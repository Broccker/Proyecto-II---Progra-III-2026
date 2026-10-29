package com.sistema.backend.config;

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
 * Configuración del backend, leída de un archivo {@code .properties}.
 *
 * <p>Orden de búsqueda al usar {@link #cargar()}:</p>
 * <ol>
 *   <li>La ruta indicada con {@code -Dbackend.config=/ruta/backend.properties}.</li>
 *   <li>La variable de entorno {@code BACKEND_CONFIG}.</li>
 *   <li>El recurso {@code /backend.properties} del classpath.</li>
 * </ol>
 *
 * <p>Además, las propiedades del sistema {@code bd.url}, {@code bd.usuario},
 * {@code bd.clave}, {@code bd.pool.tamano}, {@code servidor.puerto},
 * {@code llm.api.url}, {@code llm.api.key} y {@code llm.model} sobrescriben
 * el archivo. Así se pueden inyectar credenciales en otro equipo sin editar
 * el archivo versionado.</p>
 *
 * <p>Las credenciales nunca se escriben en el código: viven en el archivo
 * {@code backend.properties}, que está en {@code .gitignore}. La clave del
 * proveedor de IA solo existe en el backend: el frontend nunca la recibe.</p>
 */
public record ConfiguracionBackend(
        String urlBd,
        String usuarioBd,
        String claveBd,
        int puertoServidor,
        int tamanoPool,
        String urlIA,
        String claveIA,
        String modeloIA) {

    private static final String RECURSO_POR_DEFECTO = "/backend.properties";
    private static final int PUERTO_POR_DEFECTO = 5000;
    private static final int POOL_POR_DEFECTO = 10;
    private static final String MODELO_IA_POR_DEFECTO = "gpt-4o-mini";

    /**
     * Constructor de compatibilidad para configuraciones sin servicio de IA.
     */
    public ConfiguracionBackend(String urlBd, String usuarioBd, String claveBd,
                                int puertoServidor, int tamanoPool) {
        this(urlBd, usuarioBd, claveBd, puertoServidor, tamanoPool,
                "", "", MODELO_IA_POR_DEFECTO);
    }

    public ConfiguracionBackend {
        if (urlBd == null || !urlBd.startsWith("jdbc:mysql://")) {
            throw new ConfiguracionException(
                    "La URL de la base debe comenzar con jdbc:mysql:// (valor actual: " + urlBd + ").");
        }
        if (usuarioBd == null || usuarioBd.isBlank()) {
            throw new ConfiguracionException("Falta el usuario de la base de datos (bd.usuario).");
        }
        if (claveBd == null) {
            throw new ConfiguracionException("Falta la clave de la base de datos (bd.clave).");
        }
        if (puertoServidor < 1 || puertoServidor > 65535) {
            throw new ConfiguracionException(
                    "El puerto del servidor debe estar entre 1 y 65535.");
        }
        if (tamanoPool < 1 || tamanoPool > 100) {
            throw new ConfiguracionException(
                    "El tamaño del pool debe estar entre 1 y 100.");
        }
        urlIA = urlIA == null ? "" : urlIA.trim();
        claveIA = claveIA == null ? "" : claveIA.trim();
        modeloIA = modeloIA == null || modeloIA.isBlank()
                ? MODELO_IA_POR_DEFECTO : modeloIA.trim();
    }

    /**
     * Carga la configuración con el orden de búsqueda documentado en la clase.
     */
    public static ConfiguracionBackend cargar() {
        String rutaExterna = System.getProperty("backend.config");
        if (rutaExterna == null || rutaExterna.isBlank()) {
            rutaExterna = System.getenv("BACKEND_CONFIG");
        }
        return rutaExterna == null || rutaExterna.isBlank()
                ? desdeClasspath(RECURSO_POR_DEFECTO)
                : desdeArchivo(Path.of(rutaExterna));
    }

    /**
     * Carga un archivo de configuración concreto del classpath.
     * Pensado para las pruebas de integración ({@code /backend-test.properties}).
     */
    public static ConfiguracionBackend desdeClasspath(String recurso) {
        try (InputStream entrada = ConfiguracionBackend.class.getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new ConfiguracionException(
                        "No se encontró '" + recurso + "'. Copie backend.properties.example "
                                + "a backend.properties y ajuste los datos de conexión.");
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
    public static ConfiguracionBackend desdeArchivo(Path archivo) {
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

    private static ConfiguracionBackend desdePropiedades(Properties propiedades) {
        String url = valor(propiedades, "bd.url");
        if (url == null) {
            throw new ConfiguracionException("Falta la URL de la base de datos (bd.url).");
        }
        String usuario = valor(propiedades, "bd.usuario");
        if (usuario == null) {
            throw new ConfiguracionException("Falta el usuario de la base de datos (bd.usuario).");
        }
        String clave = propiedades.getProperty("bd.clave");
        if (clave == null) {
            throw new ConfiguracionException("Falta la clave de la base de datos (bd.clave).");
        }
        int puerto = entero(propiedades, "servidor.puerto", PUERTO_POR_DEFECTO);
        int pool = entero(propiedades, "bd.pool.tamano", POOL_POR_DEFECTO);
        String urlIA = valor(propiedades, "llm.api.url");
        String claveIA = valor(propiedades, "llm.api.key");
        String modeloIA = valor(propiedades, "llm.model");
        return new ConfiguracionBackend(url.trim(), usuario.trim(), clave, puerto, pool,
                urlIA, claveIA, modeloIA);
    }

    private static String valor(Properties propiedades, String clave) {
        String delSistema = System.getProperty(clave);
        String valor = delSistema != null ? delSistema : propiedades.getProperty(clave);
        return valor == null || valor.isBlank() ? null : valor;
    }

    private static int entero(Properties propiedades, String clave, int porDefecto) {
        String valor = valor(propiedades, clave);
        if (valor == null) {
            return porDefecto;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new ConfiguracionException(
                    "La propiedad '" + clave + "' debe ser un número entero (valor actual: " + valor + ").", e);
        }
    }
}
