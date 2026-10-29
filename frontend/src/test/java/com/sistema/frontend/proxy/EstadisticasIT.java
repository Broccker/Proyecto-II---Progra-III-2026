package com.sistema.frontend.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.frontend.config.ConfiguracionFrontend;
import com.sistema.frontend.controller.ControladorEstadisticas;
import com.sistema.frontend.controller.EjecutorTareas;
import com.sistema.frontend.model.ModeloEstadisticas;
import com.sistema.frontend.view.VistaEstadisticas;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import org.jfree.data.category.CategoryDataset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Estadísticas contra un servidor que habla el protocolo real.
 *
 * <p>Comprueba que el proxy transporta los conteos agregados y que la tabla y
 * el gráfico muestran exactamente las cifras que devolvió el backend. No
 * requiere MySQL: el servidor de prueba responde los conteos ya calculados.</p>
 */
@Timeout(30)
class EstadisticasIT {

    private static final LocalDate PRIMERA_SEMANA = LocalDate.of(2026, 11, 2);
    private static final LocalDate SEGUNDA_SEMANA = LocalDate.of(2026, 11, 9);

    @Test
    @DisplayName("La tabla y el gráfico muestran las cifras que envía el backend")
    void muestraLasCifrasDelBackend() throws Exception {
        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> switch (solicitud.operacion()) {
            case CATEGORIAS_RESERVADAS -> List.of(
                    new ConteoCategoria(new CategoriaRecurso("CAT-000001", "Proyectores"), 5),
                    new ConteoCategoria(new CategoriaRecurso("CAT-000002", "Salas"), 2));
            case ACTIVIDADES_POR_SEMANA -> List.of(
                    new ConteoSemana(PRIMERA_SEMANA, 3),
                    new ConteoSemana(SEGUNDA_SEMANA, 1));
            default -> null;
        })) {
            servidor.iniciar();

            try (ServiceProxy proxy = new ServiceProxy(configuracion(servidor.puerto()))) {
                servidor.esperarClientes(1);

                ModeloEstadisticas modelo = new ModeloEstadisticas();
                VistaEstadisticas vista = new VistaEstadisticas(modelo);
                ControladorEstadisticas controlador = new ControladorEstadisticas(proxy,
                        modelo, vista, sincrono());

                controlador.cargar();

                assertEquals(2, vista.getTablaRecursos().getRowCount());
                assertEquals("Proyectores", vista.getTablaRecursos().getValueAt(0, 0));
                assertEquals(5L, vista.getTablaRecursos().getValueAt(0, 1));

                CategoryDataset recursos = vista.getGraficoRecursos().getCategoryPlot()
                        .getDataset();
                assertEquals(5.0, recursos.getValue("Cantidad", "Proyectores").doubleValue());
                assertEquals(2.0, recursos.getValue("Cantidad", "Salas").doubleValue());

                assertEquals(2, vista.getTablaActividades().getRowCount());
                assertEquals("02/11/2026", vista.getTablaActividades().getValueAt(0, 0));
                assertEquals(3L, vista.getTablaActividades().getValueAt(0, 1));

                CategoryDataset semanas = vista.getGraficoActividades().getCategoryPlot()
                        .getDataset();
                assertEquals(3.0, semanas.getValue("Cantidad", "02/11/2026").doubleValue());
                assertEquals(1.0, semanas.getValue("Cantidad", "09/11/2026").doubleValue());
            }
        }
    }

    private static ConfiguracionFrontend configuracion(int puerto) {
        return new ConfiguracionFrontend("127.0.0.1", puerto, 5_000);
    }

    private static EjecutorTareas sincrono() {
        return new EjecutorTareas() {
            @Override
            public void ejecutar(Runnable trabajo, Runnable alTerminar,
                                 Consumer<Throwable> alFallar) {
                try {
                    trabajo.run();
                } catch (Throwable error) {
                    alFallar.accept(error);
                    return;
                }
                alTerminar.run();
            }
        };
    }
}
