package com.sistema.backend.server;

import com.sistema.common.protocolo.Protocolo;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * Cliente de prueba que habla exactamente el mismo protocolo que el frontend.
 */
final class ClientePrueba implements AutoCloseable {

    private final Socket socket;
    private final ObjectOutputStream salida;
    private final ObjectInputStream entrada;

    ClientePrueba(int puerto) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), puerto), 2_000);
        socket.setSoTimeout(5_000);
        salida = Protocolo.crearSalida(socket.getOutputStream());
        salida.flush();
        entrada = Protocolo.crearEntrada(socket.getInputStream());
    }

    void enviar(Object mensaje) throws IOException {
        Protocolo.enviar(salida, mensaje);
    }

    Object recibir() throws IOException, ClassNotFoundException {
        return Protocolo.recibir(entrada);
    }

    void tiempoDeEspera(int milisegundos) throws SocketException {
        socket.setSoTimeout(milisegundos);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
