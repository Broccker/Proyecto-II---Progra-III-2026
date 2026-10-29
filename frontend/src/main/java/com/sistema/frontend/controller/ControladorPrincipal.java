package com.sistema.frontend.controller;

import com.sistema.common.modelo.Rol;
import com.sistema.common.modelo.Usuario;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloActividades;
import com.sistema.frontend.model.ModeloAlertas;
import com.sistema.frontend.model.ModeloCalendarizacion;
import com.sistema.frontend.model.ModeloCategorias;
import com.sistema.frontend.model.ModeloFuncionarios;
import com.sistema.frontend.model.ModeloRecursos;
import com.sistema.frontend.model.ModeloReservas;
import com.sistema.frontend.model.Sesion;
import com.sistema.frontend.view.VentanaPrincipal;
import com.sistema.frontend.view.VistaActividades;
import com.sistema.frontend.view.VistaCalendarizacion;
import com.sistema.frontend.view.VistaCategorias;
import com.sistema.frontend.view.VistaFuncionarios;
import com.sistema.frontend.view.VistaMisReservas;
import com.sistema.frontend.view.VistaNuevaReserva;
import com.sistema.frontend.view.PanelAlertas;
import com.sistema.frontend.view.VistaRecursos;
import com.sistema.frontend.view.VistaReservas;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ensambla la ventana principal según el rol del usuario autenticado.
 *
 * <p>El administrador mantiene funcionarios, categorías, recursos y consulta
 * la calendarización; el funcionario crea y cancela sus reservas y consulta
 * las actividades. Cada pestaña la arma un controlador de módulo con su
 * propio modelo.</p>
 */
public final class ControladorPrincipal {

    private final VentanaPrincipal ventana;
    private final List<Runnable> cargas = new ArrayList<>();
    private final ControladorAlertas controladorAlertas;

    private Runnable alCambiarClave = () -> { };
    private Runnable alCerrarSesion = () -> { };
    private Runnable alSalir = () -> { };

    public ControladorPrincipal(Sesion sesion, Service servicio, EjecutorTareas ejecutor) {
        Objects.requireNonNull(sesion, "La sesión es obligatoria.");
        Objects.requireNonNull(servicio, "El servicio es obligatorio.");
        Objects.requireNonNull(ejecutor, "El ejecutor es obligatorio.");
        Usuario usuario = sesion.getUsuario();
        if (usuario == null) {
            throw new IllegalArgumentException("No hay una sesión activa.");
        }

        this.ventana = new VentanaPrincipal(usuario);

        ModeloAlertas modeloAlertas = new ModeloAlertas();
        PanelAlertas panelAlertas = new PanelAlertas(modeloAlertas);
        this.controladorAlertas = new ControladorAlertas(modeloAlertas, panelAlertas);
        ventana.agregarPanelAlertas(panelAlertas);

        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            armarAdministracion(servicio, ejecutor);
        } else {
            armarFuncionario(usuario, servicio, ejecutor);
        }

        ventana.alCambiarClave(() -> alCambiarClave.run());
        ventana.alCerrarSesion(() -> alCerrarSesion.run());
        ventana.alSalir(() -> alSalir.run());
    }

    private void armarAdministracion(Service servicio, EjecutorTareas ejecutor) {
        ModeloFuncionarios modeloFuncionarios = new ModeloFuncionarios();
        VistaFuncionarios vistaFuncionarios = new VistaFuncionarios(modeloFuncionarios);
        ControladorFuncionarios controladorFuncionarios = new ControladorFuncionarios(
                servicio, modeloFuncionarios, vistaFuncionarios, ejecutor);
        ventana.agregarPestana("Funcionarios", "Funcionarios", vistaFuncionarios);
        cargas.add(controladorFuncionarios::cargar);

        ModeloCategorias modeloCategorias = new ModeloCategorias();
        VistaCategorias vistaCategorias = new VistaCategorias(modeloCategorias);
        ControladorCategorias controladorCategorias = new ControladorCategorias(
                servicio, modeloCategorias, vistaCategorias, ejecutor);
        ventana.agregarPestana("Categorías", "Categorias", vistaCategorias);
        cargas.add(controladorCategorias::cargar);

        ModeloRecursos modeloRecursos = new ModeloRecursos();
        VistaRecursos vistaRecursos = new VistaRecursos(modeloRecursos);
        ControladorRecursos controladorRecursos = new ControladorRecursos(
                servicio, modeloRecursos, vistaRecursos, ejecutor);
        ventana.agregarPestana("Recursos", "Recursos", vistaRecursos);
        cargas.add(controladorRecursos::cargar);

        agregarConsultas(servicio, ejecutor);
    }

    private void armarFuncionario(Usuario usuario, Service servicio, EjecutorTareas ejecutor) {
        ModeloReservas modeloReservas = new ModeloReservas();
        VistaNuevaReserva vistaNueva = new VistaNuevaReserva(modeloReservas);
        VistaMisReservas vistaMisReservas = new VistaMisReservas(modeloReservas);
        ControladorReservas controladorReservas = new ControladorReservas(servicio,
                modeloReservas, vistaNueva, vistaMisReservas, usuario.getId(), ejecutor);
        ventana.agregarPestana("Reservas", "Reservas",
                new VistaReservas(vistaNueva, vistaMisReservas));
        cargas.add(controladorReservas::cargar);

        agregarConsultas(servicio, ejecutor);
    }

    private void agregarConsultas(Service servicio, EjecutorTareas ejecutor) {
        ModeloCalendarizacion modeloCalendarizacion = new ModeloCalendarizacion();
        VistaCalendarizacion vistaCalendarizacion =
                new VistaCalendarizacion(modeloCalendarizacion);
        ControladorCalendarizacion controladorCalendarizacion =
                new ControladorCalendarizacion(servicio, modeloCalendarizacion,
                        vistaCalendarizacion, ejecutor);
        ventana.agregarPestana("Calendarización", "Calendarizacion", vistaCalendarizacion);
        cargas.add(controladorCalendarizacion::cargar);

        ModeloActividades modeloActividades = new ModeloActividades();
        VistaActividades vistaActividades = new VistaActividades(modeloActividades);
        ControladorActividades controladorActividades = new ControladorActividades(
                servicio, modeloActividades, vistaActividades, ejecutor);
        ventana.agregarPestana("Actividades", "Actividades", vistaActividades);
        cargas.add(controladorActividades::cargar);
    }

    /** Muestra la ventana y lanza la carga inicial de cada pestaña. */
    public void mostrar() {
        ventana.mostrar();
        for (Runnable carga : cargas) {
            carga.run();
        }
    }

    /** Cierra la ventana principal sin disparar la acción de salida. */
    public void cerrar() {
        ventana.cerrar();
    }

    public VentanaPrincipal getVentana() {
        return ventana;
    }

    /** Controlador que el orquestador registra como escucha del proxy. */
    public ControladorAlertas getControladorAlertas() {
        return controladorAlertas;
    }

    public void alCambiarClave(Runnable accion) {
        alCambiarClave = accion == null ? () -> { } : accion;
    }

    public void alCerrarSesion(Runnable accion) {
        alCerrarSesion = accion == null ? () -> { } : accion;
    }

    public void alSalir(Runnable accion) {
        alSalir = accion == null ? () -> { } : accion;
    }
}
