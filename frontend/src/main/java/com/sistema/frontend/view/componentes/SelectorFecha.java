package com.sistema.frontend.view.componentes;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * Selector de fecha: campo de texto para escribirla y un botón que abre un
 * calendario mensual en un menú flotante.
 */
public final class SelectorFecha extends JPanel {

    public static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_MES =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es"));
    private static final String[] DIAS_SEMANA = {"L", "M", "X", "J", "V", "S", "D"};

    private final JTextField campoTexto;
    private final JButton botonCalendario;
    private final JPanel panelCalendario;
    private final JLabel etiquetaMes;
    private final JPanel zonaDias;
    private final List<JButton> botonesDia = new ArrayList<>();
    private final JPopupMenu menu;
    private YearMonth mesMostrado;
    private LocalDate fecha;

    public SelectorFecha() {
        this(LocalDate.now());
    }

    public SelectorFecha(LocalDate inicial) {
        fecha = inicial == null ? LocalDate.now() : inicial;
        mesMostrado = YearMonth.from(fecha);
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));

        campoTexto = new JTextField(10);
        campoTexto.setText(fecha.format(FORMATO));
        botonCalendario = new JButton("▼");
        botonCalendario.setPreferredSize(new Dimension(26, 20));
        botonCalendario.setMargin(new java.awt.Insets(0, 0, 0, 0));
        botonCalendario.setFont(botonCalendario.getFont().deriveFont(9f));
        botonCalendario.setFocusPainted(false);
        botonCalendario.setToolTipText("Abrir calendario");
        botonCalendario.addActionListener(e -> alternarCalendario());
        add(campoTexto);
        add(botonCalendario);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        JButton anterior = new JButton("<");
        anterior.setPreferredSize(new Dimension(26, 20));
        anterior.setMargin(new java.awt.Insets(0, 0, 0, 0));
        anterior.setFont(anterior.getFont().deriveFont(9f));
        anterior.addActionListener(e -> irMesAnterior());
        etiquetaMes = new JLabel();
        JButton siguiente = new JButton(">");
        siguiente.setPreferredSize(new Dimension(26, 20));
        siguiente.setMargin(new java.awt.Insets(0, 0, 0, 0));
        siguiente.setFont(siguiente.getFont().deriveFont(9f));
        siguiente.addActionListener(e -> irMesSiguiente());
        barra.add(anterior);
        barra.add(etiquetaMes);
        barra.add(siguiente);

        zonaDias = new JPanel(new GridLayout(0, 7));
        panelCalendario = new JPanel(new java.awt.BorderLayout());
        panelCalendario.add(barra, java.awt.BorderLayout.NORTH);
        panelCalendario.add(zonaDias, java.awt.BorderLayout.CENTER);

        menu = new JPopupMenu();
        menu.add(panelCalendario);
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        pintarCalendario();
    }

    /**
     * Devuelve la fecha escrita o elegida en el calendario.
     *
     * @throws IllegalArgumentException si el formato no es {@code dd/MM/yyyy}
     */
    public LocalDate getFecha() {
        String texto = campoTexto.getText().trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Indique la fecha.");
        }
        try {
            return LocalDate.parse(texto, FORMATO);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Formato de fecha inválido. Use dd/MM/yyyy.", ex);
        }
    }

    /** Cambia la fecha mostrada. */
    public void setFecha(LocalDate nueva) {
        if (nueva == null) {
            return;
        }
        fecha = nueva;
        campoTexto.setText(nueva.format(FORMATO));
        mesMostrado = YearMonth.from(fecha);
        pintarCalendario();
    }

    private void irMesAnterior() {
        mesMostrado = mesMostrado.minusMonths(1);
        pintarCalendario();
    }

    private void irMesSiguiente() {
        mesMostrado = mesMostrado.plusMonths(1);
        pintarCalendario();
    }

    private void seleccionarDia(int dia) {
        if (dia < 1 || dia > mesMostrado.lengthOfMonth()) {
            return;
        }
        fecha = mesMostrado.atDay(dia);
        campoTexto.setText(fecha.format(FORMATO));
        menu.setVisible(false);
        pintarCalendario();
    }

    private void alternarCalendario() {
        if (menu.isVisible()) {
            menu.setVisible(false);
            return;
        }
        pintarCalendario();
        try {
            menu.show(botonCalendario, 0, botonCalendario.getHeight());
        } catch (RuntimeException ex) {
            // Sin ventana visible (entornos headless): no hay nada que mostrar.
        }
    }

    private void pintarCalendario() {
        botonesDia.clear();
        zonaDias.removeAll();
        etiquetaMes.setText(capitalizar(mesMostrado.format(FORMATO_MES)));

        for (String dia : DIAS_SEMANA) {
            zonaDias.add(new JLabel(dia, JLabel.CENTER));
        }

        int desfase = mesMostrado.atDay(1).getDayOfWeek().getValue() - 1;
        int diasDelMes = mesMostrado.lengthOfMonth();
        for (int i = 0; i < desfase; i++) {
            zonaDias.add(new JLabel());
        }
        for (int dia = 1; dia <= diasDelMes; dia++) {
            int numero = dia;
            JButton botonDia = new JButton(String.valueOf(dia));
            botonDia.setFocusPainted(false);
            botonDia.setMargin(new java.awt.Insets(1, 1, 1, 1));
            if (mesMostrado.atDay(dia).isEqual(fecha)) {
                botonDia.setSelected(true);
            }
            botonDia.addActionListener(e -> seleccionarDia(numero));
            botonesDia.add(botonDia);
            zonaDias.add(botonDia);
        }

        zonaDias.revalidate();
        zonaDias.repaint();
        panelCalendario.revalidate();
        panelCalendario.repaint();
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
