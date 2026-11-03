package com.sistema.common.dto;

import java.io.Serializable;


public record CeldaCalendario(String texto, boolean ocupada) implements Serializable {

    public CeldaCalendario {
        texto = texto == null ? "" : texto;
    }

    public static CeldaCalendario libre() {
        return new CeldaCalendario("", false);
    }
}
