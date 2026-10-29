package com.sistema.backend.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.protocolo.Notificacion;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistroClientesTest {

    private final List<Runnable> tareas = new ArrayList<>();
    private final Executor ejecutorDiferido = tareas::add;
    private RegistroClientes registro;

    @BeforeEach
    void preparar() {
        tareas.clear();
        registro = new RegistroClientes(ejecutorDiferido);
    }

    @Test
    @DisplayName("Difunde solo a los demás clientes y deja la entrega en el ejecutor")
    void difundeSoloALosDemas() {
        ClienteFalso origen = new ClienteFalso("cliente-1");
        ClienteFalso otro1 = new ClienteFalso("cliente-2");
        ClienteFalso otro2 = new ClienteFalso("cliente-3");
        registrar(origen, otro1, otro2);
        Notificacion notificacion = DatosPrueba.notificacion();

        registro.difundir(notificacion, origen);

        assertTrue(origen.recibidos.isEmpty());
        assertTrue(otro1.recibidos.isEmpty(), "La entrega no debe ser inmediata.");
        assertTrue(otro2.recibidos.isEmpty(), "La entrega no debe ser inmediata.");
        assertEquals(2, tareas.size());

        tareas.forEach(Runnable::run);

        assertTrue(origen.recibidos.isEmpty(), "El creador no debe recibir su notificación.");
        assertEquals(List.of(notificacion), otro1.recibidos);
        assertEquals(List.of(notificacion), otro2.recibidos);
    }

    @Test
    @DisplayName("Quita y cierra al cliente cuya escritura falla")
    void quitaYcierraAlClienteQueFalla() {
        ClienteFalso origen = new ClienteFalso("cliente-1");
        ClienteFalso fallido = new ClienteFalso("cliente-2");
        fallido.fallo = new IOException("conexión cortada");
        registrar(origen, fallido);

        registro.difundir(DatosPrueba.notificacion(), origen);
        tareas.forEach(Runnable::run);

        assertEquals(1, registro.cantidad());
        assertTrue(fallido.cerrado);
        assertTrue(fallido.recibidos.isEmpty());
        assertFalse(origen.cerrado);
    }

    @Test
    @DisplayName("Cierra todas las conexiones y vacía el registro")
    void cierraTodosLosClientes() {
        ClienteFalso primero = new ClienteFalso("cliente-1");
        ClienteFalso segundo = new ClienteFalso("cliente-2");
        registrar(primero, segundo);

        registro.cerrarTodos();

        assertEquals(0, registro.cantidad());
        assertTrue(primero.cerrado);
        assertTrue(segundo.cerrado);
    }

    @Test
    @DisplayName("Elimina un cliente y tolera eliminarlo dos veces")
    void eliminaClientes() {
        ClienteFalso cliente = new ClienteFalso("cliente-1");
        registro.registrar(cliente);

        registro.eliminar(cliente);
        registro.eliminar(cliente);

        assertEquals(0, registro.cantidad());
    }

    @Test
    @DisplayName("No permite dos conexiones con el mismo identificador")
    void rechazaIdentificadoresDuplicados() {
        registro.registrar(new ClienteFalso("cliente-1"));

        assertThrows(IllegalStateException.class,
                () -> registro.registrar(new ClienteFalso("cliente-1")));
        assertEquals(1, registro.cantidad());
    }

    private void registrar(ClienteFalso... clientes) {
        for (ClienteFalso cliente : clientes) {
            registro.registrar(cliente);
        }
    }

    private static final class ClienteFalso implements ClienteConectado {

        private final String id;
        private final List<Object> recibidos = new ArrayList<>();
        private boolean cerrado;
        private IOException fallo;

        private ClienteFalso(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public void enviar(Object mensaje) throws IOException {
            if (fallo != null) {
                throw fallo;
            }
            recibidos.add(mensaje);
        }

        @Override
        public void cerrar() {
            cerrado = true;
        }

        @Override
        public boolean estaCerrado() {
            return cerrado;
        }
    }
}
