package com.sistema.frontend.model;

import com.sistema.common.protocolo.Notificacion;
import com.sistema.common.protocolo.TipoNotificacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Alertas recibidas durante la sesión activa.
 *
 * <p>Solo guarda memoria: no se escribe en disco ni se envía al backend. Se
 * vacía al cerrar la sesión. La lista está sincronizada porque una alerta
 * puede llegar desde el hilo lector del proxy.</p>
 */
public final class ModeloAlertas {

    private final List<Alerta> alertas = new ArrayList<>();

    /**
     * Agrega la reserva de una notificación.
     *
     * @return {@code true} si la alerta era nueva; {@code false} si no es de
     *         tipo {@code NUEVA_RESERVA} o si ya estaba registrada
     */
    public synchronized boolean agregar(Notificacion notificacion) {
        Objects.requireNonNull(notificacion, "La notificación es obligatoria.");
        if (notificacion.tipo() != TipoNotificacion.NUEVA_RESERVA) {
            return false;
        }
        return agregar(Alerta.desde(notificacion.comoNuevaReserva()));
    }

    /**
     * Agrega una alerta ya construida, evitando duplicados por reserva.
     */
    public synchronized boolean agregar(Alerta alerta) {
        Objects.requireNonNull(alerta, "La alerta es obligatoria.");
        if (contieneReserva(alerta.reservaId())) {
            return false;
        }
        alertas.add(alerta);
        return true;
    }

    /** Copia inmutable de las alertas vigentes, en orden de llegada. */
    public synchronized List<Alerta> getAlertas() {
        return List.copyOf(alertas);
    }

    public synchronized int total() {
        return alertas.size();
    }

    public synchronized boolean estaVacio() {
        return alertas.isEmpty();
    }

    /** Indica si ya hay una alerta para esa reserva. */
    public synchronized boolean contieneReserva(String reservaId) {
        return alertas.stream().anyMatch(alerta -> alerta.reservaId().equals(reservaId));
    }

    /** Quita una alerta de la lista local. */
    public synchronized boolean eliminar(Alerta alerta) {
        return alertas.remove(alerta);
    }

    /** Descarta todas las alertas (cierre de sesión). */
    public synchronized void vaciar() {
        alertas.clear();
    }
}
