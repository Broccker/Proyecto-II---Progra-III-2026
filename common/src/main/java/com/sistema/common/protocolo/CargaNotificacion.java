package com.sistema.common.protocolo;

import java.io.Serializable;

/**
 * Carga útil de una {@link Notificacion}.
 *
 * <p>Es una interfaz sellada para que el cliente sepa exactamente qué tipos
 * puede recibir y pueda cambiar sobre ellos sin conversiones inseguras.</p>
 */
public sealed interface CargaNotificacion extends Serializable
        permits CargaNuevaReserva {
}
