package com.sistema.common.protocolo;

import com.sistema.common.excepciones.ValidacionException;
import java.io.Serializable;

/**
 * Respuesta del backend a una {@link Solicitud}.
 *
 * <p>El {@code id} debe ser exactamente el mismo que envió el cliente. Si
 * {@code exito} es {@code true}, {@code resultado} contiene el valor devuelto
 * por {@code Service} (puede ser {@code null} en operaciones {@code void}) y
 * {@code error} es {@code null}. Si {@code exito} es {@code false}, ocurre lo
 * contrario.</p>
 */
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
