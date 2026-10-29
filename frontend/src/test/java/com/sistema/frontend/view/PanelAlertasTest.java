package com.sistema.frontend.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Recurso;
import com.sistema.frontend.model.Alerta;
import com.sistema.frontend.model.ModeloAlertas;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PanelAlertasTest {

    private ModeloAlertas modelo;
    private PanelAlertas panel;

    @BeforeEach
    void preparar() {
        modelo = new ModeloAlertas();
        panel = new PanelAlertas(modelo);
        panel.setSize(340, 400);
    }

    private static Alerta alerta(String reservaId, String encargado, String actividad) {
        return new Alerta(reservaId, encargado, actividad, LocalDate.of(2026, 11, 2),
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                List.of(new Recurso("1001", new CategoriaRecurso("CAT-000001", "Proyectores"),
                        "Proyector Epson")));
    }

    private void clicEn(int fila, int columna) {
        panel.getTabla().setBounds(0, 0, 340, 300);
        Rectangle celda = panel.getTabla().getCellRect(fila, columna, true);
        MouseEvent evento = new MouseEvent(panel.getTabla(), MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, celda.x + 2, celda.y + 2, 1, false,
                MouseEvent.BUTTON1);
        panel.getTabla().dispatchEvent(evento);
    }

    @Test
    @DisplayName("La tabla muestra la actividad con el formato Encargado - Actividad")
    void muestraLasActividades() {
        modelo.agregar(alerta("RES-000001", "Ana Pérez", "Reunión de equipo"));
        modelo.agregar(alerta("RES-000002", "Luis Rojas", "Charla"));

        panel.actualizar();

        assertEquals(2, panel.getTabla().getRowCount());
        assertEquals("Ana Pérez - Reunión de equipo",
                panel.getTabla().getValueAt(0, 0));
        assertEquals("Actividad", panel.getTabla().getColumnName(0));
        assertEquals("Ver", panel.getTabla().getColumnName(1));
        assertEquals("Desc.", panel.getTabla().getColumnName(2));
    }

    @Test
    @DisplayName("El pie informa cuántas alertas se recibieron")
    void informaElTotal() {
        panel.actualizar();
        assertTrue(panel.getEstadoConexion().getText().startsWith("Sin alertas"));

        modelo.agregar(alerta("RES-000001", "Ana Pérez", "Reunión"));
        panel.actualizar();
        assertEquals("1 alerta recibida.", panel.getEstadoConexion().getText());
    }

    @Test
    @DisplayName("El clic en la columna Ver entrega la fila al controlador")
    void clicEnVer() {
        modelo.agregar(alerta("RES-000001", "Ana Pérez", "Reunión"));
        panel.actualizar();
        AtomicInteger filaRecibida = new AtomicInteger(-1);
        panel.alVer(filaRecibida::set);

        clicEn(0, 1);

        assertEquals(0, filaRecibida.get());
    }

    @Test
    @DisplayName("El clic en la columna Desc. entrega la fila al controlador")
    void clicEnDescartar() {
        modelo.agregar(alerta("RES-000001", "Ana Pérez", "Reunión"));
        panel.actualizar();
        AtomicInteger filaRecibida = new AtomicInteger(-1);
        panel.alDescartar(filaRecibida::set);

        clicEn(0, 2);

        assertEquals(0, filaRecibida.get());
    }

    @Test
    @DisplayName("Un clic fuera de las columnas de acción no dispara nada")
    void clicEnLaActividadNoDispara() {
        modelo.agregar(alerta("RES-000001", "Ana Pérez", "Reunión"));
        panel.actualizar();
        AtomicInteger ver = new AtomicInteger(-1);
        AtomicInteger descartar = new AtomicInteger(-1);
        panel.alVer(ver::set);
        panel.alDescartar(descartar::set);

        clicEn(0, 0);

        assertEquals(-1, ver.get());
        assertEquals(-1, descartar.get());
    }

    @Test
    @DisplayName("El detalle incluye encargado, fecha, horario y recursos")
    void detalleCompleto() {
        String detalle = PanelAlertas.textoDetalle(
                alerta("RES-000001", "Ana Pérez", "Reunión de equipo"));

        assertTrue(detalle.contains("Encargado: Ana Pérez"));
        assertTrue(detalle.contains("Fecha: 02/11/2026"));
        assertTrue(detalle.contains("Desde: 09:00"));
        assertTrue(detalle.contains("Hasta: 11:00"));
        assertTrue(detalle.contains("Recursos: Proyector Epson"));
    }

    @Test
    @DisplayName("La desconexión se refleja en el pie del panel")
    void muestraLaDesconexion() {
        panel.mostrarDesconexion("Se perdió la conexión con el backend.");

        assertEquals("Sin conexión con el servidor.",
                panel.getEstadoConexion().getText());
    }
}
