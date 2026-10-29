package com.sistema.frontend.view;

import com.sistema.frontend.model.Alerta;
import com.sistema.frontend.model.ModeloAlertas;
import com.sistema.frontend.util.Iconos;
import com.sistema.frontend.view.componentes.ModeloTabla;
import com.sistema.frontend.view.componentes.SelectorFecha;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Panel de alertas de la ventana principal.
 *
 * <p>Vive a la derecha de las pestañas y siempre está visible. Solo muestra el
 * estado de {@link ModeloAlertas}: no consulta al backend ni decide cuándo se
 * descarta una alerta. Los clics en las columnas "Ver" y "Desc." se exponen
 * como acciones para que el controlador las atienda.</p>
 */
public final class PanelAlertas extends JPanel {

    private static final String TITULO = "Alertas";
    private static final int COLUMNA_ACTIVIDAD = 0;
    private static final int COLUMNA_VER = 1;
    private static final int COLUMNA_DESCARTAR = 2;
    private static final int ANCHO_PREFERIDO = 340;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ModeloAlertas modelo;
    private final ModeloTabla tablaModelo;
    private final JTable tabla;
    private final JLabel estadoConexion = new JLabel(" ");

    private IntConsumer alVer = fila -> { };
    private IntConsumer alDescartar = fila -> { };

    public PanelAlertas(ModeloAlertas modelo) {
        this.modelo = modelo;
        this.tablaModelo = new ModeloTabla("Actividad", "Ver", "Desc.");
        this.tabla = new JTable(tablaModelo);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(TITULO));
        setPreferredSize(new Dimension(ANCHO_PREFERIDO, 0));

        configurarTabla();
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(crearPie(), BorderLayout.SOUTH);
    }

    private void configurarTabla() {
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(24);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getColumnModel().getColumn(COLUMNA_ACTIVIDAD).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(COLUMNA_VER).setPreferredWidth(40);
        tabla.getColumnModel().getColumn(COLUMNA_VER).setMaxWidth(48);
        tabla.getColumnModel().getColumn(COLUMNA_DESCARTAR).setPreferredWidth(48);
        tabla.getColumnModel().getColumn(COLUMNA_DESCARTAR).setMaxWidth(56);

        Icon carpeta = Iconos.carpeta();
        Icon descartar = Iconos.boton("Eliminar");
        tabla.getColumnModel().getColumn(COLUMNA_VER)
                .setCellRenderer(new RendererIcono(carpeta, "Ver"));
        tabla.getColumnModel().getColumn(COLUMNA_DESCARTAR)
                .setCellRenderer(new RendererIcono(descartar, "Desc."));

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evento) {
                if (!SwingUtilities.isLeftMouseButton(evento)) {
                    return;
                }
                int fila = tabla.rowAtPoint(evento.getPoint());
                int columna = tabla.columnAtPoint(evento.getPoint());
                if (fila < 0) {
                    return;
                }
                if (columna == COLUMNA_VER) {
                    alVer.accept(fila);
                } else if (columna == COLUMNA_DESCARTAR) {
                    alDescartar.accept(fila);
                } else if (columna == COLUMNA_ACTIVIDAD && evento.getClickCount() == 2) {
                    alVer.accept(fila);
                }
            }
        });
    }

    private JPanel crearPie() {
        estadoConexion.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));
        JPanel pie = new JPanel(new BorderLayout());
        pie.add(estadoConexion, BorderLayout.CENTER);
        return pie;
    }

    /** Vuelca las alertas del modelo en la tabla. */
    public void actualizar() {
        List<Alerta> alertas = modelo.getAlertas();
        tablaModelo.vaciar();
        for (Alerta alerta : alertas) {
            tablaModelo.addRow(new Object[]{alerta.textoActividad(), "", ""});
        }
        estadoConexion.setToolTipText(null);
        if (tablaModelo.getRowCount() == 0) {
            estadoConexion.setText("Sin alertas en esta sesión.");
        } else {
            estadoConexion.setText(alertas.size() == 1
                    ? "1 alerta recibida." : alertas.size() + " alertas recibidas.");
        }
    }

    /** Abre el detalle de la alerta en un diálogo modal. */
    public void mostrarDetalle(Alerta alerta) {
        String detalle = textoDetalle(alerta);
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[" + alerta.actividad() + "] " + detalle);
            return;
        }

        Window propietario = SwingUtilities.getWindowAncestor(this);
        JDialog dialogo = new JDialog(propietario, alerta.actividad(),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new GridBagLayout());
        contenido.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(3, 4, 3, 4);
        restricciones.anchor = GridBagConstraints.WEST;

        agregarCampo(contenido, restricciones, 0, "Encargado:", alerta.encargado());
        agregarCampo(contenido, restricciones, 1, "Fecha:",
                alerta.fecha().format(SelectorFecha.FORMATO));
        agregarCampo(contenido, restricciones, 2, "Desde:",
                alerta.horaInicio().format(FORMATO_HORA));
        agregarCampo(contenido, restricciones, 3, "Hasta:",
                alerta.horaFin().format(FORMATO_HORA));
        agregarCampo(contenido, restricciones, 4, "Recursos:", alerta.textoRecursos());

        JButton aceptar = new JButton("OK");
        aceptar.addActionListener(evento -> dialogo.dispose());
        JPanel pie = new JPanel();
        pie.add(aceptar);
        restricciones.gridx = 0;
        restricciones.gridy = 5;
        restricciones.gridwidth = 2;
        restricciones.anchor = GridBagConstraints.CENTER;
        restricciones.insets = new Insets(10, 4, 2, 4);
        contenido.add(pie, restricciones);

        dialogo.setContentPane(contenido);
        dialogo.pack();
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    /**
     * Pide confirmación para descartar una alerta. El descarte es local: no se
     * envía ninguna petición al backend.
     */
    public boolean confirmarDescarte(Alerta alerta) {
        return Dialogos.confirmar(this, "¿Descartar?",
                "¿Descartar la alerta de la actividad \"" + alerta.actividad() + "\"?");
    }

    /**
     * Informa en el pie del panel que se perdió la conexión con el backend.
     *
     * <p>El diálogo lo muestra {@code ControladorConexion}, que es quien
     * ofrece la reconexión; así no aparecen dos avisos por el mismo corte.</p>
     */
    public void mostrarDesconexion(String mensaje) {
        estadoConexion.setText("Sin conexión con el servidor.");
        estadoConexion.setToolTipText(mensaje);
    }

    /** Restablece el estado del panel cuando la conexión vuelve. */
    public void mostrarConexion() {
        actualizar();
    }

    /** Texto del detalle, reutilizado por el diálogo y por las pruebas. */
    public static String textoDetalle(Alerta alerta) {
        return "Encargado: " + alerta.encargado()
                + "\nFecha: " + alerta.fecha().format(SelectorFecha.FORMATO)
                + "\nDesde: " + alerta.horaInicio().format(FORMATO_HORA)
                + "\nHasta: " + alerta.horaFin().format(FORMATO_HORA)
                + "\nRecursos: " + alerta.textoRecursos();
    }

    private static void agregarCampo(JPanel panel, GridBagConstraints restricciones,
                                     int fila, String etiqueta, String valor) {
        restricciones.gridx = 0;
        restricciones.gridy = fila;
        restricciones.gridwidth = 1;
        panel.add(new JLabel(etiqueta), restricciones);
        restricciones.gridx = 1;
        panel.add(new JLabel(valor), restricciones);
    }

    public void alVer(IntConsumer accion) {
        alVer = accion == null ? fila -> { } : accion;
    }

    public void alDescartar(IntConsumer accion) {
        alDescartar = accion == null ? fila -> { } : accion;
    }

    public JTable getTabla() {
        return tabla;
    }

    public JLabel getEstadoConexion() {
        return estadoConexion;
    }

    /** Renderer que dibuja un icono en la celda, con texto si falta el icono. */
    private static final class RendererIcono extends DefaultTableCellRenderer {

        private final Icon icono;
        private final String textoAlterno;

        private RendererIcono(Icon icono, String textoAlterno) {
            this.icono = icono;
            this.textoAlterno = textoAlterno;
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor,
                                                       boolean seleccionada, boolean enfocada,
                                                       int fila, int columna) {
            JLabel componente = (JLabel) super.getTableCellRendererComponent(tabla, valor,
                    seleccionada, enfocada, fila, columna);
            componente.setText(icono == null ? textoAlterno : "");
            componente.setIcon(icono);
            componente.setHorizontalAlignment(CENTER);
            componente.setToolTipText(textoAlterno);
            return componente;
        }
    }
}
