package com.sistema.frontend.controller;

import com.sistema.common.servicio.Service;
import com.sistema.frontend.view.VistaCambiarClave;
import java.util.Objects;

/**
 * Controlador del cambio de clave.
 *
 * <p>La confirmación se compara en el frontend para dar respuesta inmediata,
 * pero la longitud y la fortaleza se siguen validando en el backend. La vista
 * no decide nada: solo entrega los textos escritos.</p>
 */
public final class ControladorCambiarClave {

    private final Service servicio;
    private final VistaCambiarClave vista;
    private final EjecutorTareas ejecutor;

    private Runnable alTerminar = () -> { };
    private Runnable alCancelar = () -> { };

    public ControladorCambiarClave(Service servicio, VistaCambiarClave vista,
                                   EjecutorTareas ejecutor) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio es obligatorio.");
        this.vista = Objects.requireNonNull(vista, "La vista es obligatoria.");
        this.ejecutor = Objects.requireNonNull(ejecutor, "El ejecutor es obligatorio.");

        vista.alAceptar(this::aceptar);
        vista.alCancelar(this::cancelar);
    }

    /** Valida los campos y solicita el cambio al backend. */
    void aceptar() {
        String id = vista.getId();
        String actual = vista.getClaveActual();
        String nueva = vista.getNuevaClave();
        String confirmacion = vista.getConfirmacion();

        if (id.isBlank()) {
            vista.mostrarAdvertencia("Ingrese el identificador del usuario.");
            return;
        }
        if (actual.isBlank()) {
            vista.mostrarAdvertencia("Ingrese la clave actual.");
            return;
        }
        if (nueva.isBlank()) {
            vista.mostrarAdvertencia("Ingrese la nueva clave.");
            return;
        }
        if (!nueva.equals(confirmacion)) {
            vista.mostrarAdvertencia("La confirmación no coincide con la nueva clave.");
            return;
        }

        vista.habilitar(false);
        ejecutor.ejecutar(
                () -> servicio.cambiarClave(id, actual, nueva, confirmacion),
                () -> {
                    vista.habilitar(true);
                    vista.limpiarClaves();
                    vista.mostrarInformacion("La clave se actualizó correctamente.");
                    alTerminar.run();
                },
                error -> {
                    vista.habilitar(true);
                    vista.mostrarError(Errores.mensaje(error));
                });
    }

    void cancelar() {
        alCancelar.run();
    }

    /** Acción tras un cambio exitoso (normalmente cerrar el diálogo). */
    public void alTerminar(Runnable accion) {
        alTerminar = accion == null ? () -> { } : accion;
    }

    /** Acción del botón Cancelar. */
    public void alCancelar(Runnable accion) {
        alCancelar = accion == null ? () -> { } : accion;
    }
}
