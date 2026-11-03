package com.sistema.frontend.view.componentes;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.Locale;
import javax.swing.JFileChooser;


public final class SelectorArchivoPdf {

    private SelectorArchivoPdf() {
    }


    public static Path elegir(Component padre, String nombreSugerido) {
        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar reporte PDF");
        selector.setApproveButtonText("Guardar");
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
