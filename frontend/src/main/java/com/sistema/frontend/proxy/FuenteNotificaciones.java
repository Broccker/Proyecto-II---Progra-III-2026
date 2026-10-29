package com.sistema.frontend.proxy;

/**
 * Origen de notificaciones asincrónicas hacia el frontend.
 *
 * <p>La implementa el proxy y la consumen los controladores que necesitan
 * escuchar eventos del backend (el de alertas). El controlador no conoce los
 * sockets: solo se registra como {@link EscuchaNotificaciones}.</p>
 */
public interface FuenteNotificaciones {

    /**
     * Registra un receptor de notificaciones.
     */
    void agregarEscucha(EscuchaNotificaciones escucha);

    /**
     * Quita un receptor previamente registrado.
     */
    void quitarEscucha(EscuchaNotificaciones escucha);

    /**
     * Indica si la conexión con el backend sigue activa.
     */
    boolean estaConectado();
}
