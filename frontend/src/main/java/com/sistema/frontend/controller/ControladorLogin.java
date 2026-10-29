package com.sistema.frontend.controller;

import com.sistema.common.modelo.Usuario;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.Sesion;
import com.sistema.frontend.util.Validaciones;
import com.sistema.frontend.view.VistaLogin;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Controlador del ingreso al sistema.
 *
 * <p>Coordina la vista de login, el contrato {@link Service} y el modelo de
 * sesión. La vista solo recoge credenciales; aquí se validan localmente, se
 * consulta al backend fuera del hilo de eventos y se registra el usuario.</p>
 */
public final class ControladorLogin {

    private final Service servicio;
    private final VistaLogin vista;
    private final Sesion sesion;
    private final EjecutorTareas ejecutor;

    private Consumer<Usuario> alAutenticar = usuario -> { };
    private Consumer<String> alSolicitarCambioClave = id -> { };
    private Runnable alSalir = () -> { };

    public ControladorLogin(Service servicio, VistaLogin vista, Sesion sesion,
                            EjecutorTareas ejecutor) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio es obligatorio.");
        this.vista = Objects.requireNonNull(vista, "La vista es obligatoria.");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria.");
        this.ejecutor = Objects.requireNonNull(ejecutor, "El ejecutor es obligatorio.");

        vista.alIngresar(this::ingresar);
        vista.alCambiarClave(this::solicitarCambioClave);
        vista.alSalir(this::salir);
    }

    /** Valida el formulario, autentica y registra la sesión. */
    void ingresar() {
        String id;
        String clave;
        try {
            id = Validaciones.identificador(vista.getId(), "El identificador del usuario");
            clave = Validaciones.clave(vista.getClave(), "La clave");
        } catch (IllegalArgumentException e) {
            vista.mostrarAdvertencia(e.getMessage());
            return;
        }

        vista.habilitar(false);
        AtomicReference<Usuario> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(servicio.iniciarSesion(id, clave)),
                () -> {
                    vista.habilitar(true);
                    Usuario usuario = Objects.requireNonNull(resultado.get(),
                            "El backend no devolvió el usuario autenticado.");
                    sesion.iniciar(usuario);
                    vista.limpiarClave();
                    alAutenticar.accept(usuario);
                },
                error -> {
                    vista.habilitar(true);
                    vista.limpiarClave();
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    void solicitarCambioClave() {
        alSolicitarCambioClave.accept(vista.getId());
    }

    void salir() {
        alSalir.run();
    }

    /** Acción a ejecutar cuando el backend acepta las credenciales. */
    public void alAutenticar(Consumer<Usuario> accion) {
        alAutenticar = accion == null ? usuario -> { } : accion;
    }

    /** Acción para abrir el cambio de clave desde el login. */
    public void alSolicitarCambioClave(Consumer<String> accion) {
        alSolicitarCambioClave = accion == null ? id -> { } : accion;
    }

    /** Acción del botón Salir. */
    public void alSalir(Runnable accion) {
        alSalir = accion == null ? () -> { } : accion;
    }
}
