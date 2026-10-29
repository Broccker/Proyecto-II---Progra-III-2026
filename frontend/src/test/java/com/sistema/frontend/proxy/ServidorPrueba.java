package com.sistema.frontend.proxy;

import com.sistema.common.protocolo.Notificacion;
import com.sistema.common.protocolo.Protocolo;
import com.sistema.common.protocolo.Respuesta;
import com.sistema.common.protocolo.Solicitud;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Servidor de prueba que habla el mismo protocolo que el backend real.
 *
 * <p>Escucha en un puerto libre, atiende a cada cliente en un hilo virtual y
 * responde con el resultado que devuelve el {@link Comportamiento} inyectado.
 * Las respuestas se envían desde un hilo por petición, de modo que pueden
 * llegar en un orden distinto al de las solicitudes y así se comprueba que el
 * proxy las empareja por identificador de correlación.</p>
 *
 * <p>No depende del módulo {@code backend}: solo usa las clases de
 * {@code common}, igual que haría un doble de prueba.</p>
 */
final class ServidorPrueba implements AutoCloseable {

    /**
     * Calcula el resultado de una solicitud. Si lanza una excepción de
     * dominio, se devuelve como {@link Respuesta} fallida.
     */
    interface Comportamiento {
        Object responder(Solicitud solicitud) throws Exception;
    }

    private static final long ESPERA_MAXIMA_MS = 3_000;

    private final Comportamiento comportamiento;
    private final ExecutorService clientes = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService peticiones = Executors.newVirtualThreadPerTaskExecutor();
    private final List<Conexion> conexiones = new CopyOnWriteArrayList<>();
    private final List<Solicitud> recibidas = new CopyOnWriteArrayList<>();
    private final AtomicBoolean activo = new AtomicBoolean(false);

    private ServerSocket serverSocket;
    private Thread hiloAceptacion;

    ServidorPrueba(Comportamiento comportamiento) {
        this.comportamiento = comportamiento;
    }

    void iniciar() throws IOException {
        ServerSocket nuevo = new ServerSocket();
        nuevo.setReuseAddress(true);
        nuevo.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        serverSocket = nuevo;
        activo.set(true);
        hiloAceptacion = Thread.ofPlatform()
                .name("prueba-aceptacion")
                .daemon(true)
                .start(this::aceptar);
    }

    int puerto() {
        return serverSocket.getLocalPort();
    }

    List<Solicitud> recibidas() {
        return List.copyOf(recibidas);
    }

    void esperarClientes(int cantidad) {
        esperarHasta(() -> conexiones.size() >= cantidad,
                "No se conectaron " + cantidad + " clientes de prueba.");
    }

    void esperarSolicitudes(int cantidad) {
        esperarHasta(() -> recibidas.size() >= cantidad,
                "No llegaron " + cantidad + " solicitudes al servidor de prueba.");
    }

    /**
     * Envía una notificación a todos los clientes conectados.
     */
    void difundir(Notificacion notificacion) throws IOException {
        for (Conexion conexion : conexiones) {
            conexion.enviar(notificacion);
        }
    }

    /**
     * Cierra las conexiones activas sin detener el servidor, para simular una
     * caída de la red.
     */
    void cerrarConexiones() {
        for (Conexion conexion : conexiones) {
            conexion.close();
        }
        conexiones.clear();
    }

    @Override
    public void close() {
        activo.set(false);
        cerrarSilenciosamente(serverSocket);
        cerrarConexiones();
        clientes.shutdownNow();
        peticiones.shutdownNow();
        Thread hilo = hiloAceptacion;
        if (hilo != null && hilo != Thread.currentThread()) {
            try {
                hilo.join(TimeUnit.SECONDS.toMillis(2));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void aceptar() {
        while (activo.get()) {
            try {
                Socket socket = serverSocket.accept();
                clientes.execute(() -> atender(socket));
            } catch (IOException e) {
                if (activo.get()) {
                    throw new IllegalStateException("Falló la aceptación de la prueba.", e);
                }
                return;
            }
        }
    }

    private void atender(Socket socket) {
        Conexion conexion = null;
        try {
            conexion = new Conexion(socket);
            conexiones.add(conexion);
            while (activo.get() && !conexion.estaCerrada()) {
                Object mensaje = conexion.recibir();
                if (mensaje instanceof Solicitud solicitud) {
                    recibidas.add(solicitud);
                    Conexion destino = conexion;
                    peticiones.execute(() -> responder(destino, solicitud));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // El cliente cerró la conexión o la prueba terminó.
        } finally {
            if (conexion != null) {
                conexiones.remove(conexion);
                conexion.close();
            } else {
                cerrarSilenciosamente(socket);
            }
        }
    }

    private void responder(Conexion conexion, Solicitud solicitud) {
        try {
            Object resultado = comportamiento.responder(solicitud);
            conexion.enviar(Respuesta.ok(solicitud.id(), resultado));
        } catch (Exception e) {
            try {
                conexion.enviar(Respuesta.error(solicitud.id(), e));
            } catch (IOException ignorado) {
                // La conexión ya no existe; no hay a quién responder.
            }
        }
    }

    private static void esperarHasta(BooleanSupplier condicion, String mensaje) {
        long limite = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ESPERA_MAXIMA_MS);
        while (!condicion.getAsBoolean() && System.nanoTime() < limite) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(mensaje, e);
            }
        }
        if (!condicion.getAsBoolean()) {
            throw new IllegalStateException(mensaje);
        }
    }

    private static void cerrarSilenciosamente(ServerSocket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException e) {
            // El socket de prueba ya estaba cerrado.
        }
    }

    private static void cerrarSilenciosamente(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // El socket de prueba ya estaba cerrado.
        }
    }

    /**
     * Conexión de un cliente de prueba, con escrituras sincronizadas.
     */
    private static final class Conexion implements AutoCloseable {

        private final Socket socket;
        private final ObjectOutputStream salida;
        private final ObjectInputStream entrada;
        private final Object bloqueo = new Object();
        private final AtomicBoolean cerrada = new AtomicBoolean(false);

        private Conexion(Socket socket) throws IOException {
            this.socket = socket;
            this.salida = Protocolo.crearSalida(socket.getOutputStream());
            this.salida.flush();
            this.entrada = Protocolo.crearEntrada(socket.getInputStream());
        }

        private Object recibir() throws IOException, ClassNotFoundException {
            return Protocolo.recibir(entrada);
        }

        private void enviar(Object mensaje) throws IOException {
            synchronized (bloqueo) {
                if (cerrada.get()) {
                    throw new IOException("La conexión de prueba está cerrada.");
                }
                Protocolo.enviar(salida, mensaje);
            }
        }

        private boolean estaCerrada() {
            return cerrada.get();
        }

        @Override
        public void close() {
            if (cerrada.compareAndSet(false, true)) {
                cerrarSilenciosamente(socket);
            }
        }
    }
}
