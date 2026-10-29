package com.sistema.frontend.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.excepciones.AccesoDatosException;
import com.sistema.common.excepciones.AutenticacionException;
import com.sistema.common.excepciones.ConfiguracionException;
import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.excepciones.ServicioExternoException;
import com.sistema.common.excepciones.SistemaException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.protocolo.CodigoError;
import com.sistema.common.protocolo.ErrorRemoto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TraductorErroresTest {

    @Test
    @DisplayName("Cada código se convierte en su excepción de dominio")
    void traduceCadaCodigo() {
        assertTraduccion(CodigoError.VALIDACION, ValidacionException.class);
        assertTraduccion(CodigoError.NO_ENCONTRADO, NoEncontradoException.class);
        assertTraduccion(CodigoError.CONFLICTO, ConflictoException.class);
        assertTraduccion(CodigoError.AUTENTICACION, AutenticacionException.class);
        assertTraduccion(CodigoError.ACCESO_DATOS, AccesoDatosException.class);
        assertTraduccion(CodigoError.CONFIGURACION, ConfiguracionException.class);
        assertTraduccion(CodigoError.SERVICIO_EXTERNO, ServicioExternoException.class);
        assertTraduccion(CodigoError.INTERNO, SistemaException.class);
    }

    @Test
    @DisplayName("Conserva el mensaje que redactó el backend")
    void conservaElMensaje() {
        RuntimeException error = TraductorErrores.aExcepcion(
                new ErrorRemoto(CodigoError.CONFLICTO, "El recurso ya está reservado."));

        assertEquals("El recurso ya está reservado.", error.getMessage());
    }

    @Test
    @DisplayName("Clasifica los errores internos como no pertenecientes al dominio")
    void clasificaLosErrores() {
        assertTrue(TraductorErrores.esDeDominio(CodigoError.VALIDACION));
        assertTrue(TraductorErrores.esDeDominio(CodigoError.AUTENTICACION));
        assertEquals(false, TraductorErrores.esDeDominio(CodigoError.INTERNO));
        assertEquals(false, TraductorErrores.esDeDominio(null));
    }

    private static void assertTraduccion(CodigoError codigo,
                                         Class<? extends RuntimeException> esperado) {
        RuntimeException error = TraductorErrores.aExcepcion(
                new ErrorRemoto(codigo, "Mensaje de " + codigo));

        assertInstanceOf(esperado, error);
        assertEquals("Mensaje de " + codigo, error.getMessage());
    }
}
