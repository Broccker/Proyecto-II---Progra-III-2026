package com.sistema.frontend.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.modelo.Funcionario;
import com.sistema.common.protocolo.Operacion;
import com.sistema.common.protocolo.Solicitud;
import com.sistema.frontend.config.ConfiguracionFrontend;
import com.sistema.frontend.controller.ControladorFuncionarios;
import com.sistema.frontend.controller.ControladorLogin;
import com.sistema.frontend.controller.EjecutorTareas;
import com.sistema.frontend.model.ModeloFuncionarios;
import com.sistema.frontend.model.Sesion;
import com.sistema.frontend.view.VistaFuncionarios;
import com.sistema.frontend.view.VistaLogin;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Flujo MVC completo contra un servidor que habla el protocolo real.
 *
 * <p>Comprueba el criterio de la Fase 8: la vista recoge los datos, el
 * controlador los envía por el proxy, el "backend" responde y el modelo y la
 * tabla del frontend reflejan el resultado. No requiere MySQL.</p>
 */
@Timeout(25)
class FlujoMVCIT {

    private static final EjecutorTareas DIRECTO = (trabajo, alTerminar, alFallar) -> {
        try {
            trabajo.run();
        } catch (Throwable error) {
            alFallar.accept(error);
            return;
        }
        alTerminar.run();
    };

    @Test
    @DisplayName("Login y alta de funcionario recorren Vista → Controlador → Proxy → Servidor")
    void loginYAltaDeFuncionario() throws Exception {
        List<Funcionario> funcionarios = new CopyOnWriteArrayList<>();
        funcionarios.add(DatosPrueba.funcionario());

        try (ServidorPrueba servidor = new ServidorPrueba(solicitud -> switch (
                solicitud.operacion()) {
            case INICIAR_SESION -> DatosPrueba.funcionario();
            case BUSCAR_FUNCIONARIOS -> List.copyOf(funcionarios);
            case ALTA_FUNCIONARIO -> {
                funcionarios.add(new Funcionario((String) solicitud.parametro(0),
                        (String) solicitud.parametro(1), (String) solicitud.parametro(2)));
                yield null;
            }
            default -> throw new IllegalStateException(
                    "Operación inesperada: " + solicitud.operacion());
        })) {
            servidor.iniciar();

            try (ServiceProxy proxy = new ServiceProxy(configuracion(servidor.puerto()))) {
                Sesion sesion = new Sesion();
                VistaLogin vistaLogin = new VistaLogin();
                new ControladorLogin(proxy, vistaLogin, sesion, DIRECTO);
                vistaLogin.establecerId(DatosPrueba.ID_FUNCIONARIO);
                vistaLogin.establecerClave("222");

                vistaLogin.getBotonIngresar().doClick();

                assertTrue(sesion.haySesion());
                assertEquals(DatosPrueba.ID_FUNCIONARIO, sesion.getUsuario().getId());

                ModeloFuncionarios modelo = new ModeloFuncionarios();
                VistaFuncionarios vista = new VistaFuncionarios(modelo);
                ControladorFuncionarios controlador = new ControladorFuncionarios(proxy,
                        modelo, vista, DIRECTO);

                controlador.cargar();
                assertEquals(1, modelo.getFuncionarios().size());
                assertEquals(1, vista.getTabla().getRowCount());

                vista.establecerFormulario("444", "Pedro Pérez", "77776666");
                vista.getBotonGuardar().doClick();

                assertEquals(2, modelo.getFuncionarios().size());
                assertEquals(2, vista.getTabla().getRowCount());
                assertEquals("444", vista.getTabla().getValueAt(1, 0));

                List<Solicitud> recibidas = servidor.recibidas();
                assertTrue(recibidas.stream().anyMatch(
                        solicitud -> solicitud.operacion() == Operacion.INICIAR_SESION));
                assertTrue(recibidas.stream().anyMatch(
                        solicitud -> solicitud.operacion() == Operacion.BUSCAR_FUNCIONARIOS));
                Solicitud alta = recibidas.stream()
                        .filter(solicitud -> solicitud.operacion()
                                == Operacion.ALTA_FUNCIONARIO)
                        .findFirst()
                        .orElseThrow();
                assertEquals("444", alta.parametro(0));
                assertEquals("Pedro Pérez", alta.parametro(1));
                assertEquals("77776666", alta.parametro(2));
            }
        }
    }

    private static ConfiguracionFrontend configuracion(int puerto) {
        return new ConfiguracionFrontend("127.0.0.1", puerto, 5_000);
    }
}
