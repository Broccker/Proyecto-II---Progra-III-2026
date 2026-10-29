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

/**
 * Registro thread-safe de los clientes activos y difusión de notificaciones.
 *
 * <p>Las notificaciones se encolan en un {@link Executor}: el hilo que atendió
 * la petición no espera a que todos los frontends reciban la alerta. Si la
 * escritura falla, el cliente se elimina y se cierra, como exige el enunciado.</p>
 */
public final class RegistroClientes {

    private static final Logger LOG = LoggerFactory.getLogger(RegistroClientes.class);

    private final Map<String, ClienteConectado> clientes = new ConcurrentHashMap<>();
    private final Executor difusor;

    public RegistroClientes(Executor difusor) {
        this.difusor = Objects.requireNonNull(difusor, "El ejecutor de notificaciones es obligatorio.");
    }

    /**
     * Agrega un cliente conectado.
     *
     * @throws IllegalStateException si ya existe una conexión con el mismo id
     */
    public void registrar(ClienteConectado cliente) {
        Objects.requireNonNull(cliente, "El cliente es obligatorio.");
        ClienteConectado anterior = clientes.putIfAbsent(cliente.id(), cliente);
        if (anterior != null) {
            throw new IllegalStateException(
                    "Ya hay un cliente conectado con el id " + cliente.id() + ".");
        }
    }

    /**
     * Quita un cliente del registro. Es seguro llamarlo aunque ya no esté.
     */
    public void eliminar(ClienteConectado cliente) {
        if (cliente != null) {
            clientes.remove(cliente.id(), cliente);
        }
    }

    /**
     * Cantidad de clientes activos.
     */
    public int cantidad() {
        return clientes.size();
    }

    /**
     * Encola una notificación para todos los clientes activos menos el origen.
     *
     * <p>La comparación se hace por identidad de conexión, no por usuario: si
     * la misma persona abre dos frontends, la otra sesión sí recibe la alerta.</p>
     */
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

    /**
     * Cierra todas las conexiones y vacía el registro.
     */
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
