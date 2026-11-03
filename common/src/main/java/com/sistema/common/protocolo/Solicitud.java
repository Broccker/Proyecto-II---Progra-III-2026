package com.sistema.common.protocolo;

import com.sistema.common.excepciones.ValidacionException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


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


        parametros = Collections.unmodifiableList(new ArrayList<>(parametros));
        if (parametros.size() != operacion.cantidadParametros()) {
            throw new ValidacionException("La operación " + operacion
                    + " espera " + operacion.cantidadParametros()
                    + " parámetros, pero llegaron " + parametros.size() + ".");
        }
    }


    public static Solicitud de(Operacion operacion, Object... parametros) {
        return conId(Protocolo.nuevoId(), operacion, parametros);
    }


    public static Solicitud conId(String id, Operacion operacion, Object... parametros) {
        return new Solicitud(id, operacion,
                parametros == null ? null : Arrays.asList(parametros));
    }


    public Object parametro(int indice) {
        if (indice < 0 || indice >= parametros.size()) {
            throw new ValidacionException("La solicitud no tiene un parámetro en la posición "
                    + indice + ".");
        }
        return parametros.get(indice);
    }
}
