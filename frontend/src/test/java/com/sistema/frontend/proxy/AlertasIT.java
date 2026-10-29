package com.sistema.frontend.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.protocolo.Operacion;
import com.sistema.frontend.config.ConfiguracionFrontend;
import com.sistema.frontend.controller.ControladorAlertas;
import com.sistema.frontend.model.ModeloAlertas;
import com.sistema.frontend.view.PanelAlertas;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Área de alertas contra un servidor que habla el protocolo real.
 *
 * <p>Simula dos frontends conectados: el servidor envía la notificación solo al
 * que no creó la reserva. Comprueba que la alerta llega al modelo y a la tabla
 * en el hilo de eventos, que ver y descartar no generan tráfico y que la
 * pérdida de conexión se informa en el panel. No requiere MySQL.</p>
 */
@Timeout(30)
class AlertasIT {

    private static final long ESPERA_MS = 5_000;

    @Test
    @DisplayName("Una reserva de otro frontend llega solo al receptor")
    void laAlertaLlegaSoloAlOtroFrontend() throws Exception {
        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> null)) {
            servidor.iniciar();

            try (ServiceProxy receptor = new ServiceProxy(configuracion(servidor.puerto()));
                 ServiceProxy creador = new ServiceProxy(configuracion(servidor.puerto()))) {
                servidor.esperarClientes(2);

                ModeloAlertas modeloReceptor = new ModeloAlertas();
                PanelAlertas panelReceptor = new PanelAlertas(modeloReceptor);
                ControladorAlertas alertasReceptor =
                        new ControladorAlertas(modeloReceptor, panelReceptor);
                receptor.agregarEscucha(alertasReceptor);

                ModeloAlertas modeloCreador = new ModeloAlertas();
                ControladorAlertas alertasCreador = new ControladorAlertas(modeloCreador,
                        new PanelAlertas(modeloCreador));
                creador.agregarEscucha(alertasCreador);

                servidor.enviarA(0, DatosPrueba.notificacion());

                esperar(() -> modeloReceptor.total() == 1);
                assertEquals("Ana Pérez - Reunión de equipo",
                        modeloReceptor.getAlertas().get(0).textoActividad());
                assertEquals(1, panelReceptor.getTabla().getRowCount());
                assertEquals(0, modeloCreador.total());
            }
        }
    }

    @Test
    @DisplayName("La alerta nace de una reserva creada por el otro frontend")
    void laReservaDeOtroFrontendGeneraLaAlerta() throws Exception {
        ServidorPrueba[] referencia = new ServidorPrueba[1];
        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> {
            if (solicitud.operacion() == Operacion.CREAR_RESERVA) {
                referencia[0].enviarA(0, DatosPrueba.notificacion());
                return DatosPrueba.reserva();
            }
            return null;
        })) {
            referencia[0] = servidor;
            servidor.iniciar();

            try (ServiceProxy receptor = new ServiceProxy(configuracion(servidor.puerto()));
                 ServiceProxy creador = new ServiceProxy(configuracion(servidor.puerto()))) {
                servidor.esperarClientes(2);

                ModeloAlertas modelo = new ModeloAlertas();
                PanelAlertas panel = new PanelAlertas(modelo);
                ControladorAlertas alertas = new ControladorAlertas(modelo, panel);
                receptor.agregarEscucha(alertas);

                ModeloAlertas modeloCreador = new ModeloAlertas();
                creador.agregarEscucha(new ControladorAlertas(modeloCreador,
                        new PanelAlertas(modeloCreador)));

                creador.crearReserva("222", "Reunión de equipo", DatosPrueba.FECHA,
                        DatosPrueba.HORA_INICIO, DatosPrueba.HORA_FIN,
                        List.of(DatosPrueba.ID_CATEGORIA));

                esperar(() -> modelo.total() == 1);
                assertEquals(0, modeloCreador.total());

                int solicitudesAntes = servidor.recibidas().size();
                alertas.ver(0);
                alertas.descartar(0);

                assertTrue(modelo.estaVacio());
                assertEquals(solicitudesAntes, servidor.recibidas().size(),
                        "Ver y descartar no deben enviar peticiones al backend.");
                assertEquals(0, panel.getTabla().getRowCount());
            }
        }
    }

    @Test
    @DisplayName("La pérdida de conexión se informa en el panel de alertas")
    void laDesconexionSeInforma() throws Exception {
        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> null)) {
            servidor.iniciar();

            try (ServiceProxy proxy = new ServiceProxy(configuracion(servidor.puerto()))) {
                servidor.esperarClientes(1);

                ModeloAlertas modelo = new ModeloAlertas();
                PanelAlertas panel = new PanelAlertas(modelo);
                proxy.agregarEscucha(new ControladorAlertas(modelo, panel));

                servidor.cerrarConexiones();

                esperar(() -> panel.getEstadoConexion().getText()
                        .startsWith("Sin conexión"));
                assertTrue(panel.getEstadoConexion().getText()
                        .contains("Sin conexión con el servidor."));
            }
        }
    }

    private static ConfiguracionFrontend configuracion(int puerto) {
        return new ConfiguracionFrontend("127.0.0.1", puerto, 5_000);
    }

    private static void esperar(BooleanSupplier condicion) throws InterruptedException {
        long limite = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS
                .toNanos(ESPERA_MS);
        while (!condicion.getAsBoolean() && System.nanoTime() < limite) {
            Thread.sleep(10);
        }
        assertTrue(condicion.getAsBoolean(), "La condición no se cumplió a tiempo.");
    }
}
