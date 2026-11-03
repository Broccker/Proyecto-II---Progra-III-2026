package com.sistema.common.protocolo;

import com.sistema.common.excepciones.ValidacionException;
import java.io.Serializable;


public record Respuesta(String id, boolean exito, Object resultado, ErrorRemoto error)
        implements Serializable {

    private static final long serialVersionUID = 1L;

    public Respuesta {
        if (id == null || id.isBlank()) {
            throw new ValidacionException(
                    "El identificador de correlación de la respuesta no puede estar vacío.");
        }
        id = id.trim();
        if (exito && error != null) {
            throw new ValidacionException(
                    "Una respuesta exitosa no puede incluir un error.");
        }
        if (!exito && error == null) {
            throw new ValidacionException(
                    "Una respuesta fallida debe incluir un error.");
        }
        if (!exito && resultado != null) {
            throw new ValidacionException(
                    "Una respuesta fallida no puede incluir un resultado.");
        }
    }

    public static Respuesta ok(String id, Object resultado) {
        return new Respuesta(id, true, resultado, null);
    }

    public static Respuesta error(String id, CodigoError codigo, String mensaje) {
        return new Respuesta(id, false, null, new ErrorRemoto(codigo, mensaje));
    }

    public static Respuesta error(String id, Throwable causa) {
        return new Respuesta(id, false, null, ErrorRemoto.de(causa));
    }
}
