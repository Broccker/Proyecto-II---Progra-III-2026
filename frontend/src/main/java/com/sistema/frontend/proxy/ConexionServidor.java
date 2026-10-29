package com.sistema.frontend.proxy;

import com.sistema.common.protocolo.Protocolo;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Socket del frontend con el backend y flujos del protocolo.
 *
 * <p>El flujo de salida se crea y se vacía antes del de entrada, igual que en
 * el servidor, para que ambos extremos intercambien sus cabeceras sin
 * bloquearse. Las escrituras se serializan porque varias peticiones pueden
 * enviarse desde hilos distintos (por ejemplo, dos {@code SwingWorker}).</p>
 */
final class ConexionServidor implements AutoCloseable {

    private static final int TIEMPO_CONEXION_MS = 5_000;

    private final Socket socket;
    private final ObjectOutputStream salida;
    private final ObjectInputStream entrada;
    private final Object bloqueoSalida = new Object();
    private final AtomicBoolean cerrada = new AtomicBoolean(false);

    /**
     * Abre la conexión y prepara los flujos.
     *
     * @throws IOException si el host no responde o el protocolo no arranca
     */
    static ConexionServidor abrir(String host, int puerto) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, puerto), TIEMPO_CONEXION_MS);
            socket.setTcpNoDelay(true);
            return new ConexionServidor(socket);
        } catch (IOException e) {
            cerrarSilenciosamente(socket);
            throw e;
        }
    }

    private ConexionServidor(Socket socket) throws IOException {
        this.socket = socket;
        try {
            this.salida = Protocolo.crearSalida(socket.getOutputStream());
            this.salida.flush();
            this.entrada = Protocolo.crearEntrada(socket.getInputStream());
        } catch (IOException e) {
            cerrarSilenciosamente(socket);
            throw e;
        }
    }

    ObjectInputStream entrada() {
        return entrada;
    }

    /**
     * Escribe un mensaje completo. Varias llamadas concurrentes no mezclan
     * sus bytes.
     */
    void enviar(Object mensaje) throws IOException {
        synchronized (bloqueoSalida) {
            if (cerrada.get()) {
                throw new IOException("La conexión con el backend está cerrada.");
            }
            Protocolo.enviar(salida, mensaje);
        }
    }

    boolean estaCerrada() {
        return cerrada.get();
    }

    @Override
    public void close() {
        if (cerrada.compareAndSet(false, true)) {
            cerrarSilenciosamente(socket);
        }
    }

    private static void cerrarSilenciosamente(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // Cerrar un socket ya cerrado no es un error recuperable.
        }
    }
}
