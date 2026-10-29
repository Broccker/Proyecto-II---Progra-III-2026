package com.sistema.frontend.view.componentes;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.Locale;
import javax.swing.JFileChooser;

/**
 * Selector de archivo para exportar un reporte PDF.
 */
public final class SelectorArchivoPdf {

    private SelectorArchivoPdf() {
    }

    /**
     * Pide al usuario el destino del PDF.
     *
     * @return la ruta elegida (con extensión {@code .pdf}) o {@code null} si
     *         canceló o no hay pantalla disponible
     */
    public static Path elegir(Component padre, String nombreSugerido) {
        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new java.io.File(nombreSugerido));
        if (selector.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        Path destino = selector.getSelectedFile().toPath();
        if (!destino.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            destino = destino.resolveSibling(destino.getFileName() + ".pdf");
        }
        return destino;
    }
}
