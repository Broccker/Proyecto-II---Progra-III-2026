package com.sistema.frontend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sistema.common.excepciones.ConfiguracionException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfiguracionFrontendTest {

    @Test
    @DisplayName("Carga host, puerto y timeout desde el classpath")
    void cargaDesdeClasspath() {
        ConfiguracionFrontend configuracion =
                ConfiguracionFrontend.desdeClasspath("/frontend-test.properties");

        assertEquals("127.0.0.1", configuracion.host());
        assertEquals(5678, configuracion.puerto());
        assertEquals(2_500, configuracion.tiempoEsperaMs());
    }

    @Test
    @DisplayName("Usa el backend local en el puerto 5000 por defecto")
    void valoresPorDefecto() {
        ConfiguracionFrontend configuracion = ConfiguracionFrontend.porDefecto();

        assertEquals("127.0.0.1", configuracion.host());
        assertEquals(5000, configuracion.puerto());
        assertEquals(15_000, configuracion.tiempoEsperaMs());
    }

    @Test
    @DisplayName("Las propiedades del sistema sobrescriben el archivo")
    void lasPropiedadesDelSistemaTienenPrioridad() {
        System.setProperty("servidor.puerto", "6123");
        System.setProperty("proxy.timeout.ms", "4000");
        try {
            ConfiguracionFrontend configuracion =
                    ConfiguracionFrontend.desdeClasspath("/frontend-test.properties");

            assertEquals(6123, configuracion.puerto());
            assertEquals(4_000, configuracion.tiempoEsperaMs());
        } finally {
            System.clearProperty("servidor.puerto");
            System.clearProperty("proxy.timeout.ms");
        }
    }

    @Test
    @DisplayName("Lee un archivo del sistema de archivos")
    void leeUnArchivoExterno(@TempDir Path carpeta) throws IOException {
        Path archivo = carpeta.resolve("frontend.properties");
        Files.writeString(archivo,
                "servidor.host=192.168.1.20\nservidor.puerto=6001\nproxy.timeout.ms=8000\n",
                StandardCharsets.UTF_8);

        ConfiguracionFrontend configuracion = ConfiguracionFrontend.desdeArchivo(archivo);

        assertEquals("192.168.1.20", configuracion.host());
        assertEquals(6001, configuracion.puerto());
        assertEquals(8_000, configuracion.tiempoEsperaMs());
    }

    @Test
    @DisplayName("Rechaza un archivo que no existe")
    void rechazaArchivoInexistente(@TempDir Path carpeta) {
        assertThrows(ConfiguracionException.class,
                () -> ConfiguracionFrontend.desdeArchivo(carpeta.resolve("no-existe.properties")));
    }

    @Test
    @DisplayName("Aplica los argumentos posicionales host y puerto")
    void aplicaArgumentos() {
        ConfiguracionFrontend base = ConfiguracionFrontend.porDefecto();

        ConfiguracionFrontend conDestino =
                base.conArgumentos(new String[]{"10.0.0.5", "6001"});
        assertEquals("10.0.0.5", conDestino.host());
        assertEquals(6001, conDestino.puerto());
        assertEquals(base.tiempoEsperaMs(), conDestino.tiempoEsperaMs());

        assertEquals(base, base.conArgumentos(new String[0]));

        assertThrows(ConfiguracionException.class,
                () -> base.conArgumentos(new String[]{"a", "6001", "extra"}));
        assertThrows(ConfiguracionException.class,
                () -> base.conArgumentos(new String[]{"localhost", "ochenta"}));
    }

    @Test
    @DisplayName("Valida host, puerto y tiempo de espera")
    void validaLosValores() {
        assertThrows(ConfiguracionException.class,
                () -> new ConfiguracionFrontend(" ", 5000, 15_000));
        assertThrows(ConfiguracionException.class,
                () -> new ConfiguracionFrontend(null, 5000, 15_000));
        assertThrows(ConfiguracionException.class,
                () -> new ConfiguracionFrontend("localhost", 0, 15_000));
        assertThrows(ConfiguracionException.class,
                () -> new ConfiguracionFrontend("localhost", 65_536, 15_000));
        assertThrows(ConfiguracionException.class,
                () -> new ConfiguracionFrontend("localhost", 5000, 99));
        assertThrows(ConfiguracionException.class,
                () -> new ConfiguracionFrontend("localhost", 5000, 300_001));
    }
}
