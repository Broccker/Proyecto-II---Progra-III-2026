package com.sistema.backend.logic;

import com.sistema.common.excepciones.ValidacionException;
import java.nio.charset.StandardCharsets;


final class Validaciones {


    private static final int MAX_BYTES_CLAVE = 72;

    private Validaciones() {
    }


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
