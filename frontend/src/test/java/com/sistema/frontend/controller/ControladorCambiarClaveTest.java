package com.sistema.frontend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.view.VistaCambiarClave;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ControladorCambiarClaveTest {

    private Service servicio;
    private VistaCambiarClave vista;
    private EjecutorDirecto ejecutor;
    private ControladorCambiarClave controlador;
    private AtomicBoolean terminado;

    @BeforeEach
    void preparar() {
        servicio = mock(Service.class);
        vista = new VistaCambiarClave(DatosControladores.ID_FUNCIONARIO, false);
        ejecutor = new EjecutorDirecto();
        controlador = new ControladorCambiarClave(servicio, vista, ejecutor);
        terminado = new AtomicBoolean(false);
        controlador.alTerminar(() -> terminado.set(true));
    }

    @Test
    @DisplayName("Cambia la clave cuando la confirmación coincide")
    void cambiaLaClave() {
        vista.establecerClaves("vieja", "nueva", "nueva");

        controlador.aceptar();

        verify(servicio).cambiarClave(DatosControladores.ID_FUNCIONARIO, "vieja",
                "nueva", "nueva");
        assertTrue(terminado.get());
        assertEquals("", vista.getClaveActual());
        assertEquals("", vista.getNuevaClave());
        assertEquals("", vista.getConfirmacion());
    }

    @Test
    @DisplayName("No consulta si la confirmación no coincide")
    void exigeConfirmacionCoincidente() {
        vista.establecerClaves("vieja", "nueva", "otra");

        controlador.aceptar();

        verifyNoInteractions(servicio);
        assertFalse(terminado.get());
    }

    @Test
    @DisplayName("No consulta si algún campo obligatorio está vacío")
    void exigeLosCampos() {
        vista.establecerClaves("", "nueva", "nueva");
        controlador.aceptar();
        vista.establecerClaves("vieja", "", "");
        controlador.aceptar();
        vista.establecerId("");
        vista.establecerClaves("vieja", "nueva", "nueva");
        controlador.aceptar();

        verify(servicio, never()).cambiarClave(anyString(), anyString(),
                anyString(), anyString());
        assertFalse(terminado.get());
    }

    @Test
    @DisplayName("Un fallo del backend se informa y no cierra el formulario")
    void falloDelBackendNoTermina() {
        doThrow(new ConflictoException("La clave actual no es correcta."))
                .when(servicio).cambiarClave(anyString(), anyString(), anyString(),
                        anyString());
        vista.establecerClaves("vieja", "nueva", "nueva");

        controlador.aceptar();

        assertFalse(terminado.get());
        verify(servicio).cambiarClave(DatosControladores.ID_FUNCIONARIO, "vieja",
                "nueva", "nueva");
    }

    @Test
    @DisplayName("El botón Cancelar delega en la acción configurada")
    void cancelarDelega() {
        AtomicBoolean cancelado = new AtomicBoolean(false);
        controlador.alCancelar(() -> cancelado.set(true));

        controlador.cancelar();

        assertTrue(cancelado.get());
    }
}
