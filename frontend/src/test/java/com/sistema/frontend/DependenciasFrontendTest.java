package com.sistema.frontend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


class DependenciasFrontendTest {

    @Test
    @DisplayName("El frontend no carga el driver de MySQL ni HikariCP")
    void noDependeDeLaBaseDeDatos() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.mysql.cj.jdbc.Driver"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.zaxxer.hikari.HikariDataSource"));
    }

    @Test
    @DisplayName("El frontend no carga ninguna clase del backend")
    void noDependeDelBackend() throws IOException {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.sistema.backend.ServidorMain"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.sistema.backend.logic.ServiceImpl"));
        assertFalse(DependenciasFrontendTest.class.getClassLoader()
                        .getResources("com/sistema/backend").hasMoreElements(),
                "El classpath del frontend contiene clases del backend.");
    }
}
