package com.sistema.backend.server;

import java.io.IOException;

/**
 * Cliente conectado al servidor, visto desde el registro y el difusor.
 *
 * <p>La interfaz permite que {@link RegistroClientes} trabaje con cualquier
 * implementación: la conexión real por socket o un doble de prueba. El envío
 * debe ser seguro entre hilos porque una respuesta y una notificación pueden
 * coincidir sobre el mismo cliente.</p>
 */
public interface ClienteConectado {

    /**
     * Identificador único de la conexión dentro del servidor.
     */
    String id();

    /**
     * Escribe un mensaje completo en el socket del cliente.
     */
    void enviar(Object mensaje) throws IOException;

    /**
     * Cierra la conexión. Debe poder invocarse más de una vez.
     */
    void cerrar();

    /**
     * Indica si la conexión ya fue cerrada.
     */
    boolean estaCerrado();
}
