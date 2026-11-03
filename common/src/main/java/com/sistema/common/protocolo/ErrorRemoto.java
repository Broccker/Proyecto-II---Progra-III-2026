package com.sistema.common.protocolo;

import com.sistema.common.excepciones.SistemaException;
import java.io.Serializable;
import java.util.Objects;


public record ErrorRemoto(CodigoError codigo, String mensaje) implements Serializable {

    private static final long serialVersionUID = 1L;

    public ErrorRemoto {
        Objects.requireNonNull(codigo, "El código del error es obligatorio.");
        if (mensaje == null || mensaje.isBlank()) {
            mensaje = "Error interno del servidor.";
        }
        mensaje = mensaje.trim();
    }


    public static ErrorRemoto de(Throwable error) {
        Objects.requireNonNull(error, "La causa del error es obligatoria.");
        String mensaje = error instanceof SistemaException
                && error.getMessage() != null && !error.getMessage().isBlank()
                ? error.getMessage()
                : "Error interno del servidor.";
        return new ErrorRemoto(CodigoError.de(error), mensaje);
    }
}
