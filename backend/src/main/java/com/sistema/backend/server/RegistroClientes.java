package com.sistema.backend.server;

import com.sistema.common.protocolo.Notificacion;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class RegistroClientes {

    private static final Logger LOG = LoggerFactory.getLogger(RegistroClientes.class);

    private final Map<String, ClienteConectado> clientes = new ConcurrentHashMap<>();
    private final Executor difusor;

    public RegistroClientes(Executor difusor) {
        this.difusor = Objects.requireNonNull(difusor, "El ejecutor de notificaciones es obligatorio.");
    }


    public void registrar(ClienteConectado cliente) {
        Objects.requireNonNull(cliente, "El cliente es obligatorio.");
        ClienteConectado anterior = clientes.putIfAbsent(cliente.id(), cliente);
        if (anterior != null) {
            throw new IllegalStateException(
                    "Ya hay un cliente conectado con el id " + cliente.id() + ".");
        }
    }


    public void eliminar(ClienteConectado cliente) {
        if (cliente != null) {
            clientes.remove(cliente.id(), cliente);
        }
    }


    public int cantidad() {
        return clientes.size();
    }


    public void difundir(Notificacion notificacion, ClienteConectado origen) {
        Objects.requireNonNull(notificacion, "La notificación es obligatoria.");
        Objects.requireNonNull(origen, "El cliente de origen es obligatorio.");
        for (ClienteConectado cliente : clientes.values()) {
            if (cliente.id().equals(origen.id())) {
                continue;
            }
            try {
                difusor.execute(() -> enviarNotificacion(cliente, notificacion));
            } catch (RejectedExecutionException e) {
                LOG.debug("El ejecutor rechazó la notificación para el cliente {}.",
                        cliente.id());
            }
        }
    }


    public void cerrarTodos() {
        List<ClienteConectado> copia = new ArrayList<>(clientes.values());
        clientes.clear();
        for (ClienteConectado cliente : copia) {
            cliente.cerrar();
        }
        if (!copia.isEmpty()) {
            LOG.info("Se cerraron {} conexiones de clientes.", copia.size());
        }
    }

    private void enviarNotificacion(ClienteConectado cliente, Notificacion notificacion) {
        try {
            cliente.enviar(notificacion);
        } catch (IOException e) {
            LOG.warn("No se pudo notificar al cliente {}; se cierra su conexión.",
                    cliente.id());
            eliminar(cliente);
            cliente.cerrar();
        }
    }
}
