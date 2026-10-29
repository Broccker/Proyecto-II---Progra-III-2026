package com.sistema.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.excepciones.ConfiguracionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfiguracionBackendTest {

    @TempDir
    Path directorioTemporal;

    @Test
    @DisplayName("Carga la configuración de pruebas con sus valores")
    void cargaConfiguracionDePrueba() {
        ConfiguracionBackend configuracion =
                ConfiguracionBackend.desdeClasspath("/backend-test.properties");

        assertTrue(configuracion.urlBd().contains("proyectoII_reservas_test"));
        assertEquals("root", configuracion.usuarioBd());
        assertEquals(5099, configuracion.puertoServidor());
        assertEquals(4, configuracion.tamanoPool());
        assertEquals("", configuracion.urlIA());
        assertEquals("", configuracion.claveIA());
        assertEquals("gpt-4o-mini", configuracion.modeloIA());
    }

    @Test
    @DisplayName("Aplica valores por defecto de puerto, pool y modelo de IA")
    void aplicaValoresPorDefecto() {
        ConfiguracionBackend configuracion = new ConfiguracionBackend(
                "jdbc:mysql://127.0.0.1:3306/base", "usuario", "clave", 5000, 10);

        assertEquals(5000, configuracion.puertoServidor());
        assertEquals(10, configuracion.tamanoPool());
        assertEquals("", configuracion.urlIA());
        assertEquals("", configuracion.claveIA());
        assertEquals("gpt-4o-mini", configuracion.modeloIA());
    }

    @Test
    @DisplayName("Carga la configuración del proveedor de IA desde un archivo")
    void cargaConfiguracionIA() throws IOException {
        Path archivo = directorioTemporal.resolve("backend.properties");
        Files.writeString(archivo, """
                bd.url=jdbc:mysql://127.0.0.1:3306/base
                bd.usuario=usuario
                bd.clave=clave
                llm.api.url=https://ejemplo.test/v1/chat/completions
                llm.api.key=clave-secreta
                llm.model=modelo-prueba
                """);

        ConfiguracionBackend configuracion = ConfiguracionBackend.desdeArchivo(archivo);

        assertEquals("https://ejemplo.test/v1/chat/completions", configuracion.urlIA());
        assertEquals("clave-secreta", configuracion.claveIA());
        assertEquals("modelo-prueba", configuracion.modeloIA());
    }

    @Test
    @DisplayName("Usa el modelo por defecto si el archivo lo deja vacío")
    void usaModeloPorDefectoSiEstaVacio() throws IOException {
        Path archivo = directorioTemporal.resolve("backend.properties");
        Files.writeString(archivo, """
                bd.url=jdbc:mysql://127.0.0.1:3306/base
                bd.usuario=usuario
                bd.clave=clave
                llm.model=
                """);

        assertEquals("gpt-4o-mini",
                ConfiguracionBackend.desdeArchivo(archivo).modeloIA());
    }

    @Test
    @DisplayName("Rechaza URL, puerto y pool inválidos")
    void rechazaValoresInvalidos() {
        assertThrows(ConfiguracionException.class, () -> new ConfiguracionBackend(
                "http://127.0.0.1/base", "usuario", "clave", 5000, 10));
        assertThrows(ConfiguracionException.class, () -> new ConfiguracionBackend(
                "jdbc:mysql://127.0.0.1:3306/base", "usuario", "clave", 0, 10));
        assertThrows(ConfiguracionException.class, () -> new ConfiguracionBackend(
                "jdbc:mysql://127.0.0.1:3306/base", "usuario", "clave", 5000, 0));
    }

    @Test
    @DisplayName("Falla con un archivo de configuración inexistente")
    void rechazaArchivoInexistente() {
        assertThrows(ConfiguracionException.class,
                () -> ConfiguracionBackend.desdeArchivo(Path.of("no-existe.properties")));
    }
}
