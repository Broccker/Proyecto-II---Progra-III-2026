package com.sistema.frontend.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;


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


    public static String texto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede quedar en blanco.");
        }
        return longitud(valor.trim(), campo, maximo);
    }


    public static String opcional(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        return longitud(valor.trim(), campo, maximo);
    }


    public static String identificador(String valor, String campo) {
        return texto(valor, campo, MAX_ID);
    }


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
