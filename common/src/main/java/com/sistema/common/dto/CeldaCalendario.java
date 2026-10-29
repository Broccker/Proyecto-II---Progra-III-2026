package com.sistema.common.dto;

import java.io.Serializable;

/**
 * Contenido de una celda de las matrices de calendario y actividades.
 *
 * @param texto   texto a mostrar (vacío si la celda está libre)
 * @param ocupada indica si hay al menos una reserva en ese bloque
 */
public record CeldaCalendario(String texto, boolean ocupada) implements Serializable {

    public CeldaCalendario {
        texto = texto == null ? "" : texto;
    }

    public static CeldaCalendario libre() {
        return new CeldaCalendario("", false);
    }
}
