package com.sistema.backend.server;

import com.sistema.common.servicio.Service;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servidor concurrente del backend.
 *
 * <p>Escucha en el puerto configurado, acepta conexiones y entrega cada una a
 * un {@link ManejadorCliente} que corre en su propio hilo virtual. Los hilos
 * virtuales son ideales aquí porque cada cliente pasa la mayor parte del
 * tiempo bloqueado esperando datos del socket: el servidor puede atender
 * muchos frontends con un costo mínimo.</p>
 *
 * <p>El registro de clientes es thread-safe y se usa para difundir las
 * notificaciones. {@link #detener()} cierra primero el socket de aceptación,
 * luego las conexiones activas y por último el ejecutor, de modo que ningún
 * hilo queda bloqueado al terminar el proceso.</p>
 */
public final class Servidor implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Servidor.class);

    private static final long ESPERA_CIERRE_SEGUNDOS = 5;

    private final int puertoSolicitado;
    private final Despachador despachador;
    private final RegistroClientes registro;
    private final ExecutorService ejecutor;
    private final AtomicInteger contadorClientes = new AtomicInteger();
    private final AtomicBoolean activo = new AtomicBoolean(false);
    private final AtomicBoolean detenido = new AtomicBoolean(false);

    private volatile ServerSocket socketServidor;
    private volatile Thread hiloAceptacion;
    private volatile int puerto;

    /**
     * Crea un servidor que difunde notificaciones en su propio ejecutor.
     */
    public Servidor(int puerto, Service servicio) {
        this(puerto, servicio, null);
    }

    /**
     * Constructor para pruebas: permite inyectar el ejecutor del difusor y así
     * hacer determinista el orden de las notificaciones.
     */
    Servidor(int puerto, Service servicio, Executor difusor) {
        if (puerto < 0 || puerto > 65535) {
            throw new IllegalArgumentException("El puerto debe estar entre 0 y 65535.");
        }
        this.puertoSolicitado = puerto;
        this.puerto = puerto;
        this.despachador = new Despachador(Objects.requireNonNull(servicio,
                "El servicio es obligatorio."));
        ThreadFactory fabrica = Thread.ofVirtual().name("cliente-", 0).factory();
        this.ejecutor = Executors.newThreadPerTaskExecutor(fabrica);
        this.registro = new RegistroClientes(difusor != null ? difusor : ejecutor);
    }

    /**
     * Abre el puerto y comienza a aceptar clientes en un hilo dedicado.
     *
     * @throws IllegalStateException si el servidor ya está activo, ya fue
     *         detenido o el puerto no se puede abrir
     */
    public synchronized void iniciar() {
        if (detenido.get()) {
            throw new IllegalStateException("El servidor ya fue detenido.");
        }
        if (activo.get()) {
            throw new IllegalStateException("El servidor ya está iniciado.");
        }
        ServerSocket nuevo = null;
        try {
            nuevo = new ServerSocket();
            nuevo.setReuseAddress(true);
            nuevo.bind(new InetSocketAddress(puertoSolicitado));
        } catch (IOException e) {
            cerrarSilenciosamente(nuevo);
            throw new IllegalStateException(
                    "No se pudo abrir el puerto " + puertoSolicitado + ".", e);
        }
        socketServidor = nuevo;
        puerto = nuevo.getLocalPort();
        activo.set(true);
        hiloAceptacion = Thread.ofPlatform()
                .name("aceptacion-servidor")
                .daemon(false)
                .start(this::aceptarClientes);
        LOG.info("Servidor escuchando en el puerto {}.", puerto);
    }

    /**
     * Puerto real en el que escucha el servidor (útil cuando se solicita el 0).
     */
    public int puerto() {
        return puerto;
    }

    /**
     * Indica si el servidor está aceptando conexiones.
     */
    public boolean estaActivo() {
        return activo.get() && !detenido.get();
    }

    /**
     * Cantidad de clientes que completaron la conexión y están registrados.
     */
    public int clientesConectados() {
        return registro.cantidad();
    }

    /**
     * Espera a que termine el bucle de aceptación. Se usa en {@code main} para
     * mantener vivo el proceso hasta que se cierre el servidor.
     */
    public void esperar() throws InterruptedException {
        Thread hilo = hiloAceptacion;
        if (hilo != null) {
            hilo.join();
        }
    }

    /**
     * Apaga el servidor de forma ordenada. Es idempotente.
     */
    public synchronized void detener() {
        if (!detenido.compareAndSet(false, true)) {
            return;
        }
        activo.set(false);
        LOG.info("Deteniendo el servidor...");
        cerrarSilenciosamente(socketServidor);
        registro.cerrarTodos();
        ejecutor.shutdownNow();
        try {
            if (!ejecutor.awaitTermination(ESPERA_CIERRE_SEGUNDOS, TimeUnit.SECONDS)) {
                LOG.warn("Algunos hilos de clientes no terminaron a tiempo.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Thread hilo = hiloAceptacion;
        if (hilo != null && hilo != Thread.currentThread()) {
            try {
                hilo.join(TimeUnit.SECONDS.toMillis(ESPERA_CIERRE_SEGUNDOS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOG.info("Servidor detenido.");
    }

    @Override
    public void close() {
        detener();
    }

    private void aceptarClientes() {
        while (activo.get()) {
            Socket socket = null;
            try {
                socket = socketServidor.accept();
                socket.setTcpNoDelay(true);
                String id = "cliente-" + contadorClientes.incrementAndGet();
                LOG.info("Conexión aceptada: {}.", id);
                Socket conexion = socket;
                ejecutor.execute(() ->
                        new ManejadorCliente(conexion, id, despachador, registro).run());
            } catch (SocketException e) {
                if (activo.get()) {
                    LOG.error("Se interrumpió el socket de aceptación.", e);
                }
                break;
            } catch (IOException e) {
                if (activo.get()) {
                    LOG.error("Error al aceptar una conexión.", e);
                }
                cerrarSilenciosamente(socket);
            } catch (RejectedExecutionException e) {
                LOG.warn("No se pudo atender una conexión nueva; se cierra.");
                cerrarSilenciosamente(socket);
            }
        }
        LOG.info("Bucle de aceptación finalizado.");
    }

    private static void cerrarSilenciosamente(Closeable recurso) {
        if (recurso == null) {
            return;
        }
        try {
            recurso.close();
        } catch (IOException e) {
            LOG.debug("No se pudo cerrar un recurso del servidor.", e);
        }
    }
}
