package com.sistema.frontend.util;

import java.awt.Image;
import java.net.URL;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Iconos de la interfaz, cargados de los recursos del módulo frontend.
 *
 * <p>Si un icono no está disponible se devuelve {@code null}: los componentes
 * Swing lo aceptan y la aplicación sigue funcionando.</p>
 */
public final class Iconos {

    private static final int TAMANO_ICONO_PESTANA = 20;
    private static final int TAMANO_ICONO_BOTON = 16;
    private static final String RUTA_ICONOS = "/Iconos/";

    private Iconos() {
    }

    /** Icono para una pestaña. */
    public static Icon pestana(String nombre) {
        return cargar(nombre, TAMANO_ICONO_PESTANA);
    }

    /** Icono para un botón. */
    public static Icon boton(String nombre) {
        return cargar("botones/" + nombre, TAMANO_ICONO_BOTON);
    }

    private static Icon cargar(String subruta, int tamanio) {
        URL recurso = Iconos.class.getResource(RUTA_ICONOS + subruta + "_icon.png");
        if (recurso == null) {
            return null;
        }
        ImageIcon imagen = new ImageIcon(Objects.requireNonNull(recurso));
        Image escalada = imagen.getImage().getScaledInstance(
                tamanio, tamanio, Image.SCALE_SMOOTH);
        return new ImageIcon(escalada);
    }
}
