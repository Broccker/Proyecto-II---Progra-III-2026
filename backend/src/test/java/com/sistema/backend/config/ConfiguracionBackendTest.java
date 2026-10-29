package com.sistema.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.excepciones.ConfiguracionException;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfiguracionBackendTest {

    @Test
    @DisplayName("Carga la configuración de pruebas con sus valores")
    void cargaConfiguracionDePrueba() {
        ConfiguracionBackend configuracion =
                ConfiguracionBackend.desdeClasspath("/backend-test.properties");

        assertTrue(configuracion.urlBd().contains("proyectoII_reservas_test"));
        assertEquals("root", configuracion.usuarioBd());
        assertEquals(5099, configuracion.puertoServidor());
        assertEquals(4, configuracion.tamanoPool());
    }

    @Test
    @DisplayName("Aplica valores por defecto de puerto y pool")
    void aplicaValoresPorDefecto() {
        ConfiguracionBackend configuracion = new ConfiguracionBackend(
                "jdbc:mysql://127.0.0.1:3306/base", "usuario", "clave", 5000, 10);

        assertEquals(5000, configuracion.puertoServidor());
        assertEquals(10, configuracion.tamanoPool());
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
