package com.sistema.frontend.util;

import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.UIManager;

public final class Localizacion {

    private Localizacion() {
    }

    public static void aplicar() {
        Locale locale = Locale.forLanguageTag("es-CR");
        Locale.setDefault(locale);
        JComponent.setDefaultLocale(locale);

        UIManager.put("OptionPane.okButtonText", "Aceptar");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        UIManager.put("OptionPane.yesButtonText", "Sí");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("OptionPane.closeButtonText", "Cerrar");

        UIManager.put("FileChooser.openButtonText", "Abrir");
        UIManager.put("FileChooser.saveButtonText", "Guardar");
        UIManager.put("FileChooser.cancelButtonText", "Cancelar");
        UIManager.put("FileChooser.openDialogTitleText", "Abrir archivo");
        UIManager.put("FileChooser.saveDialogTitleText", "Guardar archivo");
        UIManager.put("FileChooser.lookInLabelText", "Buscar en:");
        UIManager.put("FileChooser.fileNameLabelText", "Nombre del archivo:");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Tipo de archivo:");
        UIManager.put("FileChooser.upFolderToolTipText", "Carpeta superior");
        UIManager.put("FileChooser.homeFolderToolTipText", "Inicio");
        UIManager.put("FileChooser.newFolderToolTipText", "Nueva carpeta");
        UIManager.put("FileChooser.listViewButtonToolTipText", "Lista");
        UIManager.put("FileChooser.detailsViewButtonToolTipText", "Detalles");
        UIManager.put("FileChooser.acceptAllFileFilterText", "Todos los archivos");
    }
}
