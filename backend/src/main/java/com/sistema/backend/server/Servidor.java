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


    public Servidor(int puerto, Service servicio) {
        this(puerto, servicio, null);
    }


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


    public int puerto() {
        return puerto;
    }


    public boolean estaActivo() {
        return activo.get() && !detenido.get();
    }


    public int clientesConectados() {
        return registro.cantidad();
    }


    public void esperar() throws InterruptedException {
        Thread hilo = hiloAceptacion;
        if (hilo != null) {
            hilo.join();
        }
    }


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
