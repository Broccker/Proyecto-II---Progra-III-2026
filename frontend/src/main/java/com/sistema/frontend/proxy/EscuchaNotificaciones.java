package com.sistema.frontend.proxy;

import com.sistema.common.protocolo.Notificacion;

/**
 * Receptor de las notificaciones asincrónicas del backend.
 *
 * <p>El proxy lo invoca desde su hilo lector. En Swing, la implementación
 * debe reenviar el cambio a la interfaz con {@code SwingUtilities.invokeLater}
 * (o el mecanismo equivalente), para no modificar componentes fuera del hilo
 * de eventos.</p>
 *
 * <p>En la Fase 9 el controlador de alertas implementa esta interfaz.</p>
 */
public interface EscuchaNotificaciones {

    /**
     * Se invoca al recibir una notificación del servidor.
     */
    void alRecibir(Notificacion notificacion);

    /**
     * Se invoca si la conexión se pierde mientras el proxy está activo.
     * Por defecto no hace nada.
     */
    default void alPerderConexion(Throwable causa) {
        // Las vistas que no muestran alertas no necesitan reaccionar.
    }
}
