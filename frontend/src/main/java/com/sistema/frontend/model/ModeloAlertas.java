package com.sistema.frontend.model;

import com.sistema.common.protocolo.Notificacion;
import com.sistema.common.protocolo.TipoNotificacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public final class ModeloAlertas {

    private final List<Alerta> alertas = new ArrayList<>();


    public synchronized boolean agregar(Notificacion notificacion) {
        Objects.requireNonNull(notificacion, "La notificación es obligatoria.");
        if (notificacion.tipo() != TipoNotificacion.NUEVA_RESERVA) {
            return false;
        }
        return agregar(Alerta.desde(notificacion.comoNuevaReserva()));
    }


    public synchronized boolean agregar(Alerta alerta) {
        Objects.requireNonNull(alerta, "La alerta es obligatoria.");
        if (contieneReserva(alerta.reservaId())) {
            return false;
        }
        alertas.add(alerta);
        return true;
    }


    public synchronized List<Alerta> getAlertas() {
        return List.copyOf(alertas);
    }

    public synchronized int total() {
        return alertas.size();
    }

    public synchronized boolean estaVacio() {
        return alertas.isEmpty();
    }


    public synchronized boolean contieneReserva(String reservaId) {
        return alertas.stream().anyMatch(alerta -> alerta.reservaId().equals(reservaId));
    }


    public synchronized boolean eliminar(Alerta alerta) {
        return alertas.remove(alerta);
    }


    public synchronized void vaciar() {
        alertas.clear();
    }
}
