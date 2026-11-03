package com.sistema.frontend.util;

import java.awt.Image;
import java.net.URL;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.ImageIcon;


public final class Iconos {

    private static final int TAMANO_ICONO_PESTANA = 20;
    private static final int TAMANO_ICONO_BOTON = 16;
    private static final String RUTA_ICONOS = "/Iconos/";

    private Iconos() {
    }


    public static Icon pestana(String nombre) {
        return cargar(nombre, TAMANO_ICONO_PESTANA);
    }


    public static Icon boton(String nombre) {
        return cargar("botones/" + nombre, TAMANO_ICONO_BOTON);
    }


    public static Icon carpeta() {
        return javax.swing.UIManager.getIcon("FileView.directoryIcon");
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
