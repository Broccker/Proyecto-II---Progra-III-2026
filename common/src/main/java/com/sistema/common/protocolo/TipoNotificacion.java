package com.sistema.common.protocolo;

/**
 * Tipos de notificación asincrónica que el backend puede empujar a los
 * frontends conectados.
 */
public enum TipoNotificacion {

    /**
     * Otro funcionario creó una reserva. La carga es una
     * {@link CargaNuevaReserva}.
     */
    NUEVA_RESERVA
}
