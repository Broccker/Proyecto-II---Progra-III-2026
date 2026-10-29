package com.sistema.frontend.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Validaciones de los formularios del frontend.
 *
 * <p>Repiten localmente las reglas del backend para que el usuario reciba el
 * aviso antes de enviar la petición. El backend sigue siendo la autoridad: si
 * una regla cambia allí, esta clase solo evita viajes innecesarios y nunca
 * sustituye la comprobación del servidor.</p>
 *
 * <p>Todos los métodos lanzan {@link IllegalArgumentException} con un mensaje
 * listo para mostrarse en un diálogo.</p>
 */
public final class Validaciones {

    public static final int MAX_ID = 20;
    public static final int MAX_NOMBRE = 100;
    public static final int MAX_DESCRIPCION_CATEGORIA = 100;
    public static final int MAX_DESCRIPCION_RECURSO = 150;
    public static final int MAX_ACTIVIDAD = 150;
    public static final int MAX_FRASE_IA = 1000;
    public static final int MAX_BYTES_CLAVE = 72;

    private static final Pattern TELEFONO = Pattern.compile("^\\+?[0-9]{7,15}$");

    private Validaciones() {
    }

    /**
     * Exige un texto no vacío, lo recorta y comprueba su longitud máxima.
     */
    public static String texto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede quedar en blanco.");
        }
        return longitud(valor.trim(), campo, maximo);
    }

    /**
     * Valida un texto opcional. Un valor vacío se devuelve como cadena vacía.
     */
    public static String opcional(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        return longitud(valor.trim(), campo, maximo);
    }

    /**
     * Exige un identificador no vacío y con la longitud de la columna.
     */
    public static String identificador(String valor, String campo) {
        return texto(valor, campo, MAX_ID);
    }

    /**
     * Exige un teléfono con 7 a 15 dígitos y un {@code +} opcional al inicio.
     */
    public static String telefono(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El teléfono no puede estar vacío.");
        }
        String limpio = valor.trim();
        if (!TELEFONO.matcher(limpio).matches()) {
            throw new IllegalArgumentException(
                    "El teléfono no es válido. Use solo dígitos (7 a 15), "
                            + "con un + opcional al inicio.");
        }
        return limpio;
    }

    /**
     * Valida una clave sin recortarla: los espacios pueden ser parte de ella.
     */
    public static String clave(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacía.");
        }
        if (valor.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_CLAVE) {
            throw new IllegalArgumentException(
                    campo + " no puede superar los " + MAX_BYTES_CLAVE + " bytes.");
        }
        return valor;
    }

    private static String longitud(String valor, String campo, int maximo) {
        if (valor.length() > maximo) {
            throw new IllegalArgumentException(
                    campo + " no puede superar los " + maximo + " caracteres.");
        }
        return valor;
    }
}
