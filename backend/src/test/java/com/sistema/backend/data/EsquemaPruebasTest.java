package com.sistema.backend.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EsquemaPruebasTest {

    @Test
    @DisplayName("Quita la base de la URL conservando host y opciones")
    void quitaLaBaseDeLaUrl() {
        assertEquals("jdbc:mysql://127.0.0.1:3306/?useSSL=false&serverTimezone=UTC",
                EsquemaPruebas.urlSinBase(
                        "jdbc:mysql://127.0.0.1:3306/proyectoII_reservas_test"
                                + "?useSSL=false&serverTimezone=UTC"));
    }

    @Test
    @DisplayName("Agrega la barra final si la URL no trae base")
    void agregaBarraSiNoHayBase() {
        assertEquals("jdbc:mysql://127.0.0.1:3306/?useSSL=false",
                EsquemaPruebas.urlSinBase("jdbc:mysql://127.0.0.1:3306?useSSL=false"));
        assertEquals("jdbc:mysql://127.0.0.1:3306/",
                EsquemaPruebas.urlSinBase("jdbc:mysql://127.0.0.1:3306"));
    }
}
