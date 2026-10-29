package com.sistema.frontend.proxy;

/**
 * Origen de conexión que puede volver a abrirse tras una caída del backend.
 *
 * <p>Lo implementa el proxy y lo usa el frontend para ofrecer al usuario una
 * reconexión sin reiniciar la aplicación.</p>
 */
public interface Reconectable {

    /**
     * Reemplaza la conexión actual por una nueva.
     *
     * <p>Las peticiones pendientes se fallan antes de cerrar el socket. Si el
     * backend sigue apagado se lanza {@link ExcepcionRed} y el proxy queda
     * desconectado, listo para un nuevo intento.</p>
     */
    void reconectar();
}
