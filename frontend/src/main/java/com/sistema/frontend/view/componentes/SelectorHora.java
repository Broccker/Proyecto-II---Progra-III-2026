package com.sistema.frontend.view.componentes;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Selector de hora: campo de texto para escribirla y un botón que abre una
 * lista de horas en un menú flotante.
 */
public final class SelectorHora extends JPanel {

    private final JTextField campoTexto;
    private final JButton botonHora;
    private final JList<LocalTime> listaHoras;
    private final JPopupMenu menu;
    private LocalTime hora;

    public SelectorHora() {
        this(LocalTime.of(9, 0));
    }

    public SelectorHora(LocalTime inicial) {
        hora = inicial == null ? LocalTime.of(9, 0) : ajustar(inicial);
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));

        campoTexto = new JTextField(6);
        campoTexto.setText(hora.toString());
        botonHora = new JButton("▼");
        botonHora.setPreferredSize(new Dimension(26, 20));
        botonHora.setMargin(new java.awt.Insets(0, 0, 0, 0));
        botonHora.setFont(botonHora.getFont().deriveFont(9f));
        botonHora.setFocusPainted(false);
        botonHora.setToolTipText("Abrir lista de horas");
        botonHora.addActionListener(e -> alternarPanel());
        add(campoTexto);
        add(botonHora);

        List<LocalTime> horas = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            horas.add(LocalTime.of(i, 0));
        }
        listaHoras = new JList<>(horas.toArray(new LocalTime[0]));
        listaHoras.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaHoras.setVisibleRowCount(12);
        listaHoras.setSelectedValue(hora, true);
        listaHoras.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                LocalTime seleccionada = listaHoras.getSelectedValue();
                if (seleccionada != null) {
                    seleccionarHora(seleccionada);
                }
            }
        });

        JScrollPane panelLista = new JScrollPane(listaHoras);
        panelLista.setPreferredSize(new Dimension(150, 300));

        menu = new JPopupMenu();
        menu.add(panelLista);
    }

    /**
     * Devuelve la hora escrita o elegida.
     *
     * @throws IllegalArgumentException si el formato no es {@code HH:mm}
     */
    public LocalTime getHora() {
        String texto = campoTexto.getText().trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Indique la hora.");
        }
        try {
            return texto.contains(":")
                    ? LocalTime.parse(texto)
                    : LocalTime.of(Integer.parseInt(texto), 0);
        } catch (DateTimeException | NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Formato de hora inválido. Use HH:mm.", ex);
        }
    }

    /** Cambia la hora mostrada. */
    public void setHora(LocalTime nueva) {
        if (nueva == null) {
            return;
        }
        LocalTime ajustada = ajustar(nueva);
        listaHoras.setSelectedValue(ajustada, true);
        seleccionarHora(ajustada);
    }

    private void alternarPanel() {
        if (menu.isVisible()) {
            menu.setVisible(false);
            return;
        }
        try {
            menu.show(botonHora, 0, botonHora.getHeight());
        } catch (RuntimeException ex) {
            // Sin ventana visible (entornos headless): no hay nada que mostrar.
        }
    }

    private void seleccionarHora(LocalTime nueva) {
        hora = nueva;
        campoTexto.setText(nueva.toString());
        menu.setVisible(false);
        revalidate();
        repaint();
    }

    private static LocalTime ajustar(LocalTime valor) {
        int horaDelDia = valor.getMinute() >= 30 ? valor.getHour() + 1 : valor.getHour();
        if (horaDelDia >= 24) {
            horaDelDia = 23;
        }
        return LocalTime.of(horaDelDia, 0);
    }
}
