package com.sistema.frontend.view;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * Panel de reservas de un funcionario: alta arriba e historial abajo.
 */
public final class VistaReservas extends JPanel {

    public VistaReservas(VistaNuevaReserva vistaNueva, VistaMisReservas vistaMisReservas) {
        setLayout(new BorderLayout());
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                vistaNueva, vistaMisReservas);
        division.setResizeWeight(0.62);
        add(division, BorderLayout.CENTER);
    }
}
