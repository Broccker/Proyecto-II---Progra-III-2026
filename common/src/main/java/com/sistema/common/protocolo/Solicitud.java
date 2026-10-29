package com.sistema.common.protocolo;

import com.sistema.common.excepciones.ValidacionException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Petición que el frontend envía al backend.
 *
 * <p>El {@code id} de correlación lo genera el cliente y el servidor debe
 * devolverlo intacto en la {@link Respuesta}. Los parámetros viajan en el
 * mismo orden en que aparecen en el método de {@code Service}; se admite
 * {@code null} como valor de un parámetro.</p>
 */
public record Solicitud(String id, Operacion operacion, List<Object> parametros)
        implements Serializable {

    private static final long serialVersionUID = 1L;

    public Solicitud {
        if (id == null || id.isBlank()) {
            throw new ValidacionException(
                    "El identificador de correlación de la solicitud no puede estar vacío.");
        }
        id = id.trim();
        if (operacion == null) {
            throw new ValidacionException("La operación de la solicitud es obligatoria.");
        }
        if (parametros == null) {
            throw new ValidacionException("La lista de parámetros de la solicitud es obligatoria.");
        }
        // Se copia con ArrayList porque List.copyOf/List.of rechazan null,
        // y hay operaciones legítimas con parámetros nulos.
        parametros = Collections.unmodifiableList(new ArrayList<>(parametros));
        if (parametros.size() != operacion.cantidadParametros()) {
            throw new ValidacionException("La operación " + operacion
                    + " espera " + operacion.cantidadParametros()
                    + " parámetros, pero llegaron " + parametros.size() + ".");
        }
    }

    /**
     * Crea una solicitud con un identificador de correlación nuevo.
     */
    public static Solicitud de(Operacion operacion, Object... parametros) {
        return conId(Protocolo.nuevoId(), operacion, parametros);
    }

    /**
     * Crea una solicitud con un identificador de correlación conocido
     * (útil para pruebas y para reintentos controlados).
     */
    public static Solicitud conId(String id, Operacion operacion, Object... parametros) {
        return new Solicitud(id, operacion,
                parametros == null ? null : Arrays.asList(parametros));
    }

    /**
     * Devuelve un parámetro por posición.
     */
    public Object parametro(int indice) {
        if (indice < 0 || indice >= parametros.size()) {
            throw new ValidacionException("La solicitud no tiene un parámetro en la posición "
                    + indice + ".");
        }
        return parametros.get(indice);
    }
}
