package com.sistema.frontend.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.excepciones.AutenticacionException;
import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Reserva;
import com.sistema.common.modelo.Usuario;
import com.sistema.common.protocolo.Notificacion;
import com.sistema.common.protocolo.Operacion;
import com.sistema.common.protocolo.Solicitud;
import com.sistema.frontend.config.ConfiguracionFrontend;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Pruebas de integración del {@link ServiceProxy} contra un servidor de
 * prueba que habla el protocolo real.
 *
 * <p>Cubren el criterio de la Fase 7: la conexión, el emparejamiento de
 * respuestas por identificador, las notificaciones asincrónicas y los fallos
 * de red. No requieren MySQL ni el módulo backend.</p>
 */
@Timeout(25)
class ServiceProxyIT {

    @Test
    @DisplayName("Inicia sesión y recibe el usuario autenticado")
    void iniciaSesionYRecibeElUsuario() throws Exception {
        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> {
            if (solicitud.operacion() == Operacion.INICIAR_SESION) {
                return DatosPrueba.funcionario();
            }
            throw new IllegalStateException("Operación inesperada: " + solicitud.operacion());
        })) {
            servidor.iniciar();
            try (ServiceProxy proxy = new ServiceProxy(configuracion(servidor.puerto()))) {
                assertTrue(proxy.estaConectado());

                Usuario usuario = proxy.iniciarSesion(DatosPrueba.ID_FUNCIONARIO, "clave");

                Funcionario funcionario = assertInstanceOf(Funcionario.class, usuario);
                assertEquals(DatosPrueba.ID_FUNCIONARIO, funcionario.getId());
                assertEquals(DatosPrueba.ENCARGADO, funcionario.getNombre());

                Solicitud recibida = servidor.recibidas().getFirst();
                assertEquals(Operacion.INICIAR_SESION, recibida.operacion());
                assertEquals(DatosPrueba.ID_FUNCIONARIO, recibida.parametro(0));
                assertEquals("clave", recibida.parametro(1));
            }
        }
    }

    @Test
    @DisplayName("Envía fechas, horas y listas en el orden que espera el backend")
    void enviaLosParametrosEnOrden() throws Exception {
        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> {
            if (solicitud.operacion() == Operacion.CREAR_RESERVA) {
                return DatosPrueba.reserva();
            }
            throw new IllegalStateException("Operación inesperada: " + solicitud.operacion());
        })) {
            servidor.iniciar();
            try (ServiceProxy proxy = new ServiceProxy(configuracion(servidor.puerto()))) {
                Reserva reserva = proxy.crearReserva(DatosPrueba.ID_FUNCIONARIO,
                        DatosPrueba.ACTIVIDAD, DatosPrueba.FECHA, DatosPrueba.HORA_INICIO,
                        DatosPrueba.HORA_FIN, List.of(DatosPrueba.ID_CATEGORIA));

                assertEquals(DatosPrueba.ID_RESERVA, reserva.getId());
                Solicitud recibida = servidor.recibidas().getFirst();
                assertEquals(Operacion.CREAR_RESERVA, recibida.operacion());
                assertEquals(DatosPrueba.FECHA, recibida.parametro(2));
                assertEquals(DatosPrueba.HORA_INICIO, recibida.parametro(3));
                assertEquals(List.of(DatosPrueba.ID_CATEGORIA), recibida.parametro(5));
            }
        }
    }

    @Test
    @DisplayName("Devuelve listas tipadas y admite operaciones sin resultado")
    void devuelveListasYOperacionesSinResultado() throws Exception {
        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> switch (
                solicitud.operacion()) {
            case LISTAR_CATEGORIAS -> List.of(DatosPrueba.categoria());
            case CANCELAR_RESERVA -> null;
            default -> throw new IllegalStateException(
                    "Operación inesperada: " + solicitud.operacion());
        })) {
            servidor.iniciar();
            try (ServiceProxy proxy = new ServiceProxy(configuracion(servidor.puerto()))) {
                List<CategoriaRecurso> categorias = proxy.listarCategorias();
                assertEquals(List.of(DatosPrueba.categoria()), categorias);

                proxy.cancelarReserva(DatosPrueba.ID_RESERVA, DatosPrueba.ID_FUNCIONARIO);
                assertEquals(2, servidor.recibidas().size());
                assertEquals(Operacion.CANCELAR_RESERVA,
                        servidor.recibidas().get(1).operacion());
            }
        }
    }

    @Test
    @DisplayName("Empareja respuestas por id aunque lleguen en desorden")
    void emparejaRespuestasPorId() throws Exception {
        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> {
            if (solicitud.operacion() == Operacion.LISTAR_CATEGORIAS) {
                Thread.sleep(500);
                return List.of(DatosPrueba.categoria());
            }
            if (solicitud.operacion() == Operacion.LISTAR_FUNCIONARIOS) {
                return List.of(DatosPrueba.funcionario());
            }
            throw new IllegalStateException("Operación inesperada: " + solicitud.operacion());
        })) {
            servidor.iniciar();
            try (ServiceProxy proxy = new ServiceProxy(configuracion(servidor.puerto()));
                 ExecutorService ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {

                Future<List<CategoriaRecurso>> lenta =
                        ejecutor.submit(proxy::listarCategorias);
                servidor.esperarSolicitudes(1);

                // La segunda respuesta se envía antes que la primera.
                List<Funcionario> funcionarios = proxy.listarFuncionarios();
                assertEquals(List.of(DatosPrueba.funcionario()), funcionarios);

                List<CategoriaRecurso> categorias = lenta.get(5, TimeUnit.SECONDS);
                assertEquals(List.of(DatosPrueba.categoria()), categorias);
            }
        }
    }

    @Test
    @DisplayName("Recibe las notificaciones en las escuchas registradas")
    void recibeNotificaciones() throws Exception {
        Notificacion esperada = DatosPrueba.notificacion();
        CountDownLatch recibida = new CountDownLatch(1);
        AtomicReference<Notificacion> capturada = new AtomicReference<>();

        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> null)) {
            servidor.iniciar();
            try (ServiceProxy proxy = new ServiceProxy(configuracion(servidor.puerto()))) {
                proxy.agregarEscucha(new EscuchaNotificaciones() {
                    @Override
                    public void alRecibir(Notificacion notificacion) {
                        capturada.set(notificacion);
                        recibida.countDown();
                    }
                });
                servidor.esperarClientes(1);

                servidor.difundir(esperada);

                assertTrue(recibida.await(3, TimeUnit.SECONDS),
                        "La notificación no llegó a la escucha.");
                assertEquals(esperada, capturada.get());
                assertEquals(DatosPrueba.ID_RESERVA,
                        capturada.get().comoNuevaReserva().reservaId());
            }
        }
    }

    @Test
    @DisplayName("Convierte los errores del backend en excepciones de dominio")
    void traduceErroresDeDominio() throws Exception {
        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> {
            if (solicitud.operacion() == Operacion.INICIAR_SESION) {
                throw new AutenticacionException("Credenciales incorrectas.");
            }
            throw new ConflictoException("El funcionario tiene reservas.");
        })) {
            servidor.iniciar();
            try (ServiceProxy proxy = new ServiceProxy(configuracion(servidor.puerto()))) {
                AutenticacionException autenticacion = assertThrows(
                        AutenticacionException.class,
                        () -> proxy.iniciarSesion(DatosPrueba.ID_FUNCIONARIO, "mala"));
                assertEquals("Credenciales incorrectas.", autenticacion.getMessage());

                ConflictoException conflicto = assertThrows(ConflictoException.class,
                        () -> proxy.eliminarFuncionario(DatosPrueba.ID_FUNCIONARIO));
                assertEquals("El funcionario tiene reservas.", conflicto.getMessage());
            }
        }
    }

    @Test
    @DisplayName("Falla con un mensaje claro cuando el servidor no responde")
    void fallaPorTiempoDeEspera() throws Exception {
        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> {
            Thread.sleep(1_500);
            return List.of();
        })) {
            servidor.iniciar();
            try (ServiceProxy proxy = new ServiceProxy(
                    new ConfiguracionFrontend("127.0.0.1", servidor.puerto(), 300))) {

                ExcepcionRed error = assertThrows(ExcepcionRed.class,
                        proxy::listarCategorias);

                assertEquals(ExcepcionRed.Motivo.TIEMPO_AGOTADO, error.motivo());
                assertTrue(error.getMessage().contains("LISTAR_CATEGORIAS"));
                assertTrue(error.getMessage().contains("300"));
            }
        }
    }

    @Test
    @DisplayName("Falla al conectar si el backend está apagado")
    void fallaSiElServidorNoEstaDisponible() throws Exception {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0, 1,
                InetAddress.getLoopbackAddress())) {
            puertoLibre = socket.getLocalPort();
        }

        ExcepcionRed error = assertThrows(ExcepcionRed.class,
                () -> new ServiceProxy(configuracion(puertoLibre)));

        assertEquals(ExcepcionRed.Motivo.SIN_CONEXION, error.motivo());
        assertTrue(error.getMessage().contains(":" + puertoLibre));
        assertTrue(error.getMessage().contains("servidor"));
    }

    @Test
    @DisplayName("Falla la petición pendiente y avisa si se cae la conexión")
    void fallaLaPeticionPendienteSiSeCaeLaConexion() throws Exception {
        CountDownLatch perdida = new CountDownLatch(1);
        AtomicReference<Throwable> causa = new AtomicReference<>();

        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> {
            Thread.sleep(2_000);
            return List.of();
        })) {
            servidor.iniciar();
            try (ServiceProxy proxy = new ServiceProxy(configuracion(servidor.puerto()));
                 ExecutorService ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {

                proxy.agregarEscucha(new EscuchaNotificaciones() {
                    @Override
                    public void alRecibir(Notificacion notificacion) {
                        // No se esperan notificaciones en esta prueba.
                    }

                    @Override
                    public void alPerderConexion(Throwable motivo) {
                        causa.set(motivo);
                        perdida.countDown();
                    }
                });

                Future<ExcepcionRed> llamada = ejecutor.submit(
                        () -> assertThrows(ExcepcionRed.class, proxy::listarCategorias));
                servidor.esperarSolicitudes(1);

                servidor.cerrarConexiones();

                ExcepcionRed error = llamada.get(5, TimeUnit.SECONDS);
                assertEquals(ExcepcionRed.Motivo.CONEXION_PERDIDA, error.motivo());
                assertTrue(perdida.await(3, TimeUnit.SECONDS),
                        "La escucha no fue avisada de la pérdida de conexión.");
                assertTrue(causa.get() != null);
                assertFalse(proxy.estaConectado());
                assertEquals(ExcepcionRed.Motivo.CONEXION_PERDIDA,
                        assertThrows(ExcepcionRed.class, proxy::listarCategorias).motivo());
            }
        }
    }

    @Test
    @DisplayName("Detecta una respuesta con un tipo inesperado")
    void detectaRespuestasInvalidas() throws Exception {
        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> "no es un usuario")) {
            servidor.iniciar();
            try (ServiceProxy proxy = new ServiceProxy(configuracion(servidor.puerto()))) {
                ExcepcionRed error = assertThrows(ExcepcionRed.class,
                        () -> proxy.iniciarSesion(DatosPrueba.ID_FUNCIONARIO, "clave"));

                assertEquals(ExcepcionRed.Motivo.RESPUESTA_INVALIDA, error.motivo());
                assertTrue(error.getMessage().contains("Usuario"));
            }
        }
    }

    @Test
    @DisplayName("El cierre es idempotente y rechaza nuevas peticiones")
    void cierraLaConexion() throws Exception {
        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> null)) {
            servidor.iniciar();
            ServiceProxy proxy = new ServiceProxy(configuracion(servidor.puerto()));

            proxy.close();
            proxy.close();

            assertFalse(proxy.estaConectado());
            ExcepcionRed error = assertThrows(ExcepcionRed.class,
                    proxy::listarCategorias);
            assertEquals(ExcepcionRed.Motivo.CERRADO, error.motivo());
        }
    }

    private static ConfiguracionFrontend configuracion(int puerto) {
        return new ConfiguracionFrontend("127.0.0.1", puerto, 5_000);
    }
}
