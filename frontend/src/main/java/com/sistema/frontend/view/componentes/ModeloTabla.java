package com.sistema.frontend.view.componentes;

import javax.swing.table.DefaultTableModel;

/**
 * Modelo de tabla no editable, reutilizado por las vistas de listados.
 */
public final class ModeloTabla extends DefaultTableModel {

    public ModeloTabla(String... columnas) {
        super(columnas, 0);
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    /** Elimina todas las filas conservando los encabezados. */
    public void vaciar() {
        setRowCount(0);
    }
}
