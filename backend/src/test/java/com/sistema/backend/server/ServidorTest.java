package com.sistema.backend.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.sistema.common.servicio.Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class ServidorTest {

    @Test
    @DisplayName("Abre un puerto libre, informa el puerto real y se detiene")
    void abreYDetieneElServidor() {
        Servidor servidor = new Servidor(0, mock(Service.class));
        try {
            servidor.iniciar();

            assertTrue(servidor.estaActivo());
            assertTrue(servidor.puerto() > 0);
            assertEquals(0, servidor.clientesConectados());
        } finally {
            servidor.detener();
        }

        assertFalse(servidor.estaActivo());
        assertEquals(0, servidor.clientesConectados());
    }

    @Test
    @DisplayName("No permite iniciar dos veces ni reutilizar un servidor detenido")
    void validaElCicloDeVida() {
        Servidor servidor = new Servidor(0, mock(Service.class));
        try {
            servidor.iniciar();
            assertThrows(IllegalStateException.class, servidor::iniciar);
        } finally {
            servidor.detener();
        }

        assertThrows(IllegalStateException.class, servidor::iniciar);
        servidor.detener();
    }

    @Test
    @DisplayName("Rechaza puertos fuera de rango")
    void validaElPuerto() {
        Service servicio = mock(Service.class);

        assertThrows(IllegalArgumentException.class, () -> new Servidor(-1, servicio));
        assertThrows(IllegalArgumentException.class, () -> new Servidor(65536, servicio));
    }
}
