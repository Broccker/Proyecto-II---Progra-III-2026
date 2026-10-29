package com.sistema.backend.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import com.sistema.common.protocolo.CargaNuevaReserva;
import com.sistema.common.protocolo.CodigoError;
import com.sistema.common.protocolo.Notificacion;
import com.sistema.common.protocolo.Operacion;
import com.sistema.common.protocolo.Respuesta;
import com.sistema.common.protocolo.Solicitud;
import com.sistema.common.protocolo.TipoNotificacion;
import com.sistema.common.servicio.Service;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Prueba de integración del servidor con dos clientes reales por socket.
 *
 * <p>No usa MySQL: el {@link Service} se simula con Mockito y devuelve una
 * reserva. Así la prueba verifica el transporte, el enrutamiento por
 * operación, la respuesta con el mismo id de correlación y el broadcast
 * selectivo, que es el criterio de aceptación de la Fase 6.</p>
 */
@Timeout(20)
class ServidorIT {

    private Service servicio;
    private Servidor servidor;
    private final List<ClientePrueba> clientes = new ArrayList<>();

    @BeforeEach
    void iniciarServidor() {
        servicio = mock(Service.class);
        // Ejecutor directo: la difusión ocurre dentro del hilo del manejador,
        // lo que hace determinista la comprobación de que el creador no recibe
        // su propia notificación.
        servidor = new Servidor(0, servicio, Runnable::run);
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
        servidor.detener();
    }

    @Test
    @DisplayName("El creador recibe su respuesta y los demás la notificación")
    void elCreadorNoRecibeSuPropiaNotificacion() throws Exception {
        when(servicio.crearReserva(eq(DatosPrueba.ID_FUNCIONARIO), eq(DatosPrueba.ACTIVIDAD),
                eq(DatosPrueba.FECHA), eq(DatosPrueba.HORA_INICIO), eq(DatosPrueba.HORA_FIN),
                eq(List.of(DatosPrueba.ID_CATEGORIA))))
                .thenReturn(DatosPrueba.reserva());

        ClientePrueba creador = conectar();
        ClientePrueba otro = conectar();
        esperarClientes(2);

        Solicitud solicitud = solicitudDeReserva();
        creador.enviar(solicitud);

        Respuesta respuesta = assertInstanceOf(Respuesta.class, creador.recibir());
        assertTrue(respuesta.exito());
        assertEquals(solicitud.id(), respuesta.id());
        Reserva reserva = assertInstanceOf(Reserva.class, respuesta.resultado());
        assertEquals(DatosPrueba.ID_RESERVA, reserva.getId());

        Notificacion notificacion =
                assertInstanceOf(Notificacion.class, otro.recibir());
        assertEquals(TipoNotificacion.NUEVA_RESERVA, notificacion.tipo());
        CargaNuevaReserva carga = notificacion.comoNuevaReserva();
        assertEquals(DatosPrueba.ID_RESERVA, carga.reservaId());
        assertEquals(DatosPrueba.ENCARGADO, carga.encargado());
        assertEquals(DatosPrueba.ACTIVIDAD, carga.actividad());
        assertEquals(DatosPrueba.FECHA, carga.fecha());
        assertEquals(DatosPrueba.HORA_INICIO, carga.horaInicio());
        assertEquals(DatosPrueba.HORA_FIN, carga.horaFin());
        assertEquals(List.of("1001"),
                carga.recursos().stream().map(Recurso::getId).toList());

        creador.tiempoDeEspera(400);
        assertThrows(SocketTimeoutException.class, creador::recibir,
                "El creador no debe recibir una notificación de su propia reserva.");
    }

    @Test
    @DisplayName("Responde con el mismo id de correlación en una operación de consulta")
    void respondeConElIdDeCorrelacion() throws Exception {
        CategoriaRecurso categoria = new CategoriaRecurso(DatosPrueba.ID_CATEGORIA,
                "Proyectores");
        when(servicio.listarCategorias()).thenReturn(List.of(categoria));
        ClientePrueba cliente = conectar();
        esperarClientes(1);

        Solicitud solicitud = Solicitud.de(Operacion.LISTAR_CATEGORIAS);
        cliente.enviar(solicitud);

        Respuesta respuesta = assertInstanceOf(Respuesta.class, cliente.recibir());
        assertTrue(respuesta.exito());
        assertEquals(solicitud.id(), respuesta.id());
        assertEquals(List.of(categoria), respuesta.resultado());
    }

    @Test
    @DisplayName("Traduce un error del servicio y no difunde ninguna notificación")
    void traduceErroresSinNotificar() throws Exception {
        when(servicio.crearReserva(eq(DatosPrueba.ID_FUNCIONARIO), eq(DatosPrueba.ACTIVIDAD),
                eq(DatosPrueba.FECHA), eq(DatosPrueba.HORA_INICIO), eq(DatosPrueba.HORA_FIN),
                eq(List.of(DatosPrueba.ID_CATEGORIA))))
                .thenThrow(new ConflictoException("No hay recursos disponibles."));

        ClientePrueba creador = conectar();
        ClientePrueba otro = conectar();
        esperarClientes(2);

        Solicitud solicitud = solicitudDeReserva();
        creador.enviar(solicitud);

        Respuesta respuesta = assertInstanceOf(Respuesta.class, creador.recibir());
        assertFalse(respuesta.exito());
        assertEquals(solicitud.id(), respuesta.id());
        assertEquals(CodigoError.CONFLICTO, respuesta.error().codigo());
        assertEquals("No hay recursos disponibles.", respuesta.error().mensaje());

        otro.tiempoDeEspera(400);
        assertThrows(SocketTimeoutException.class, otro::recibir,
                "Una reserva fallida no debe generar notificaciones.");
    }

    @Test
    @DisplayName("El apagado cierra el puerto y las conexiones activas")
    void elApagadoCierraTodo() throws Exception {
        ClientePrueba cliente = conectar();
        esperarClientes(1);
        int puerto = servidor.puerto();

        servidor.detener();

        assertFalse(servidor.estaActivo());
        assertEquals(0, servidor.clientesConectados());
        cliente.tiempoDeEspera(1_000);
        assertThrows(IOException.class, cliente::recibir);
        assertThrows(IOException.class, () -> conectarA(puerto));
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

    private static Solicitud solicitudDeReserva() {
        return Solicitud.de(Operacion.CREAR_RESERVA, DatosPrueba.ID_FUNCIONARIO,
                DatosPrueba.ACTIVIDAD, DatosPrueba.FECHA, DatosPrueba.HORA_INICIO,
                DatosPrueba.HORA_FIN, List.of(DatosPrueba.ID_CATEGORIA));
    }

    private static void conectarA(int puerto) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), puerto),
                    500);
        }
    }
}
