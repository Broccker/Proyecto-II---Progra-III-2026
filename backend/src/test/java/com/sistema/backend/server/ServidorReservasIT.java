package com.sistema.backend.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.sistema.backend.config.ConfiguracionBackend;
import com.sistema.backend.data.PruebaBDBase;
import com.sistema.backend.logic.ServiceImpl;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Reserva;
import com.sistema.common.protocolo.CargaNuevaReserva;
import com.sistema.common.protocolo.Notificacion;
import com.sistema.common.protocolo.Operacion;
import com.sistema.common.protocolo.Respuesta;
import com.sistema.common.protocolo.Solicitud;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Prueba de punta a punta del servidor con el {@link ServiceImpl} real y MySQL.
 *
 * <p>Dos clientes por socket: uno crea una reserva que realmente se guarda en
 * la base de pruebas y el otro recibe la notificación. El creador no debe
 * recibirla. Si MySQL no está disponible, la prueba se omite igual que el
 * resto de las pruebas de integración.</p>
 */
@Timeout(25)
class ServidorReservasIT extends PruebaBDBase {

    private static final String ID_FUNCIONARIO = "222";
    private static final String ID_CATEGORIA = "CAT-000001";
    private static final String ID_RECURSO = "1001";

    private Servidor servidor;
    private final List<ClientePrueba> clientes = new ArrayList<>();

    @BeforeEach
    void iniciarServidor() {
        crearFuncionario(ID_FUNCIONARIO, "Ana Pérez", "88881111");
        CategoriaRecurso categoria = crearCategoria(ID_CATEGORIA, "Proyectores");
        crearRecurso(ID_RECURSO, categoria, "Proyector Epson");

        ConfiguracionBackend configuracion =
                ConfiguracionBackend.desdeClasspath("/backend-test.properties");
        servidor = new Servidor(0, new ServiceImpl(fuente(), configuracion), Runnable::run);
        servidor.iniciar();
    }

    @AfterEach
    void cerrarTodo() {
        for (ClientePrueba cliente : clientes) {
            try {
                cliente.close();
            } catch (IOException e) {
                // El servidor pudo haber cerrado ya la conexión.
            }
        }
        if (servidor != null) {
            servidor.detener();
        }
    }

    @Test
    @DisplayName("Una reserva real se guarda y solo el otro cliente recibe la alerta")
    void reservaRealNotificaAlOtroCliente() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(1);
        LocalTime horaInicio = LocalTime.of(9, 0);
        LocalTime horaFin = LocalTime.of(11, 0);

        ClientePrueba creador = conectar();
        ClientePrueba otro = conectar();
        esperarClientes(2);

        Solicitud solicitud = Solicitud.de(Operacion.CREAR_RESERVA, ID_FUNCIONARIO,
                "Actividad de prueba", fecha, horaInicio, horaFin, List.of(ID_CATEGORIA));
        creador.enviar(solicitud);

        Respuesta respuesta = assertInstanceOf(Respuesta.class, creador.recibir());
        assertTrue(respuesta.exito(),
                () -> "El servidor rechazó la reserva: " + respuesta.error());
        assertEquals(solicitud.id(), respuesta.id());
        Reserva reserva = assertInstanceOf(Reserva.class, respuesta.resultado());
        assertEquals(ID_FUNCIONARIO, reserva.getFuncionario().getId());
        assertEquals("Actividad de prueba", reserva.getActividad());
        assertEquals(fecha, reserva.getFecha());
        assertEquals(1, reserva.getRecursos().size());
        assertEquals(ID_RECURSO, reserva.getRecursos().getFirst().getId());

        Notificacion notificacion =
                assertInstanceOf(Notificacion.class, otro.recibir());
        CargaNuevaReserva carga = notificacion.comoNuevaReserva();
        assertEquals(reserva.getId(), carga.reservaId());
        assertEquals("Ana Pérez", carga.encargado());
        assertEquals("Actividad de prueba", carga.actividad());
        assertEquals(fecha, carga.fecha());
        assertEquals(horaInicio, carga.horaInicio());
        assertEquals(horaFin, carga.horaFin());
        assertEquals(List.of(ID_RECURSO),
                carga.recursos().stream().map(recurso -> recurso.getId()).toList());

        creador.tiempoDeEspera(400);
        assertThrows(SocketTimeoutException.class, creador::recibir,
                "El creador no debe recibir la notificación de su propia reserva.");
    }

    private ClientePrueba conectar() throws IOException {
        ClientePrueba cliente = new ClientePrueba(servidor.puerto());
        clientes.add(cliente);
        return cliente;
    }

    private void esperarClientes(int cantidad) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (servidor.clientesConectados() < cantidad && System.nanoTime() < limite) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("La prueba fue interrumpida mientras esperaba los clientes.");
            }
        }
        assertEquals(cantidad, servidor.clientesConectados(),
                "Los clientes no se registraron a tiempo.");
    }
}
