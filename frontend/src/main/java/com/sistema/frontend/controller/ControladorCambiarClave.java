package com.sistema.frontend.controller;

import com.sistema.common.servicio.Service;
import com.sistema.frontend.util.Validaciones;
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
        String id;
        String actual;
        String nueva;
        String confirmacion;
        try {
            id = Validaciones.identificador(vista.getId(), "El identificador del usuario");
            actual = Validaciones.clave(vista.getClaveActual(), "La clave actual");
            nueva = Validaciones.clave(vista.getNuevaClave(), "La clave nueva");
            confirmacion = vista.getConfirmacion();
        } catch (IllegalArgumentException e) {
            vista.mostrarAdvertencia(e.getMessage());
            return;
        }
        if (!nueva.equals(confirmacion)) {
            vista.mostrarAdvertencia("La confirmación no coincide con la clave nueva.");
            return;
        }
        if (nueva.equals(actual)) {
            vista.mostrarAdvertencia("La clave nueva debe ser distinta de la actual.");
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
