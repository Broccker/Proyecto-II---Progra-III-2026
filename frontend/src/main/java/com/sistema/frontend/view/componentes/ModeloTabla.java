package com.sistema.frontend.view.componentes;

import javax.swing.table.DefaultTableModel;


public final class ModeloTabla extends DefaultTableModel {

    public ModeloTabla(String... columnas) {
        super(columnas, 0);
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }


    public void vaciar() {
        setRowCount(0);
    }
}
