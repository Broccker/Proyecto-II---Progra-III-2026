package com.sistema.backend.logic;

import com.sistema.common.excepciones.ValidacionException;
import java.nio.charset.StandardCharsets;

/**
 * Validaciones comunes de la capa lógica.
 *
 * <p>Se concentran aquí para que todos los servicios devuelvan mensajes
 * homogéneos y para comprobar los límites de las columnas antes de llegar a
 * MySQL, de modo que el usuario reciba una explicación clara y no un error
 * de truncamiento.</p>
 */
final class Validaciones {

    /** BCrypt solo considera los primeros 72 bytes de la clave. */
    private static final int MAX_BYTES_CLAVE = 72;

    private Validaciones() {
    }

    /**
     * Exige un texto no vacío, lo recorta y comprueba su longitud máxima.
     */
    static String texto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionException(campo + " no puede quedar en blanco.");
        }
        String limpio = valor.trim();
        longitud(limpio, maximo, campo);
        return limpio;
    }

    static void longitud(String valor, int maximo, String campo) {
        if (valor != null && valor.length() > maximo) {
            throw new ValidacionException(
                    campo + " no puede superar los " + maximo + " caracteres.");
        }
    }

    /**
     * Valida una clave sin recortarla (los espacios pueden ser parte de ella).
     */
    static void clave(String clave, String campo) {
        if (clave == null || clave.isBlank()) {
            throw new ValidacionException(campo + " no puede estar vacía.");
        }
        if (clave.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_CLAVE) {
            throw new ValidacionException(
                    campo + " no puede superar los " + MAX_BYTES_CLAVE + " bytes.");
        }
    }
}
