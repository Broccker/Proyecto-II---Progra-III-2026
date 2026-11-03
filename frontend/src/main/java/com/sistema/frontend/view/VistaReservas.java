package com.sistema.frontend.view;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;


public final class VistaReservas extends JPanel {

    public VistaReservas(VistaNuevaReserva vistaNueva, VistaMisReservas vistaMisReservas) {
        setLayout(new BorderLayout());
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                vistaNueva, vistaMisReservas);
        division.setResizeWeight(0.62);
        add(division, BorderLayout.CENTER);
    }
}
