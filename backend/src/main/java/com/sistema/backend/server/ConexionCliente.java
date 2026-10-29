package com.sistema.backend.server;

import com.sistema.common.protocolo.Protocolo;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Conexión con un frontend: socket, flujos del protocolo e identificador.
 *
 * <p>Las escrituras se serializan con {@link #bloqueoSalida} porque el hilo que
 * atiende la petición y el hilo que difunde una notificación pueden querer
 * escribir en el mismo socket. Sin ese bloqueo los dos flujos se mezclarían y
 * el cliente no podría deserializar los mensajes.</p>
 */
public final class ConexionCliente implements ClienteConectado {

    private static final Logger LOG = LoggerFactory.getLogger(ConexionCliente.class);

    private final Socket socket;
    private final String id;
    private final ObjectOutputStream salida;
    private final ObjectInputStream entrada;
    private final Object bloqueoSalida = new Object();
    private final AtomicBoolean cerrada = new AtomicBoolean(false);

    /**
     * Crea los flujos del protocolo. El flujo de salida se crea y se vacía
     * antes del de entrada para que ambos extremos intercambien sus cabeceras
     * sin quedarse bloqueados.
     */
    public ConexionCliente(Socket socket, String id) throws IOException {
        this.socket = Objects.requireNonNull(socket, "El socket del cliente es obligatorio.");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador del cliente no puede estar vacío.");
        }
        this.id = id;
        try {
            this.salida = Protocolo.crearSalida(socket.getOutputStream());
            this.salida.flush();
            this.entrada = Protocolo.crearEntrada(socket.getInputStream());
        } catch (IOException e) {
            cerrarSilenciosamente(socket);
            throw e;
        }
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void enviar(Object mensaje) throws IOException {
        synchronized (bloqueoSalida) {
            if (cerrada.get()) {
                throw new IOException("La conexión del cliente " + id + " está cerrada.");
            }
            Protocolo.enviar(salida, mensaje);
        }
    }

    /**
     * Lee el siguiente mensaje enviado por el cliente.
     */
    public Object leer() throws IOException, ClassNotFoundException {
        return Protocolo.recibir(entrada);
    }

    @Override
    public void cerrar() {
        if (cerrada.compareAndSet(false, true)) {
            cerrarSilenciosamente(socket);
        }
    }

    @Override
    public boolean estaCerrado() {
        return cerrada.get();
    }

    private static void cerrarSilenciosamente(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOG.debug("No se pudo cerrar el socket del cliente.", e);
        }
    }
}
