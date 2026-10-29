package com.sistema.common.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.dto.ConteoSemana;
import com.sistema.common.excepciones.AccesoDatosException;
import com.sistema.common.excepciones.AutenticacionException;
import com.sistema.common.excepciones.ConfiguracionException;
import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.excepciones.ServicioExternoException;
import com.sistema.common.excepciones.ValidacionException;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RespuestaTest extends PruebaProtocoloBase {

    @Test
    @DisplayName("Serializa una respuesta exitosa con su resultado")
    void serializaRespuestaExitosa() {
        Respuesta original = Respuesta.ok("corr-1", new ConteoSemana(LocalDate.of(2026, 3, 2), 4));

        Respuesta recibida = idaYVuelta(original);

        assertEquals(original, recibida);
        assertTrue(recibida.exito());
        assertEquals(4, ((ConteoSemana) recibida.resultado()).cantidad());
        assertNull(recibida.error());
    }

    @Test
    @DisplayName("Una operación void viaja como éxito con resultado nulo")
    void admiteResultadoNuloEnExito() {
        Respuesta recibida = idaYVuelta(Respuesta.ok("corr-2", null));

        assertTrue(recibida.exito());
        assertNull(recibida.resultado());
        assertNull(recibida.error());
    }

    @Test
    @DisplayName("Serializa una respuesta fallida con su código y mensaje")
    void serializaRespuestaFallida() {
        Respuesta original = Respuesta.error("corr-3",
                new ConflictoException("No hay recursos disponibles en: Proyectores."));

        Respuesta recibida = idaYVuelta(original);

        assertEquals(original, recibida);
        assertFalse(recibida.exito());
        assertNull(recibida.resultado());
        assertEquals(CodigoError.CONFLICTO, recibida.error().codigo());
        assertEquals("No hay recursos disponibles en: Proyectores.",
                recibida.error().mensaje());
    }

    @ParameterizedTest
    @MethodSource("excepcionesDeDominio")
    @DisplayName("Traduce cada excepción de dominio a su código de error")
    void traduceExcepcionesDeDominio(Throwable excepcion, CodigoError esperado) {
        Respuesta respuesta = Respuesta.error("corr", excepcion);

        assertFalse(respuesta.exito());
        assertEquals(esperado, respuesta.error().codigo());
        assertEquals(excepcion.getMessage(), respuesta.error().mensaje());
    }

    @Test
    @DisplayName("Un error inesperado no filtra detalles técnicos")
    void ocultaDetallesDeErroresInternos() {
        Respuesta respuesta = Respuesta.error("corr",
                new IllegalStateException("SELECT * FROM usuarios falló"));

        assertEquals(CodigoError.INTERNO, respuesta.error().codigo());
        assertEquals("Error interno del servidor.", respuesta.error().mensaje());
    }

    @Test
    @DisplayName("Valida las invariantes de éxito y error")
    void validaInvariantes() {
        ErrorRemoto error = new ErrorRemoto(CodigoError.VALIDACION, "dato inválido");

        assertThrows(ValidacionException.class,
                () -> new Respuesta("corr", true, null, error));
        assertThrows(ValidacionException.class,
                () -> new Respuesta("corr", false, null, null));
        assertThrows(ValidacionException.class,
                () -> new Respuesta("corr", false, "resultado", error));
        assertThrows(ValidacionException.class,
                () -> new Respuesta("  ", true, null, null));
    }

    @Test
    @DisplayName("Un mensaje de error vacío se reemplaza por uno genérico")
    void normalizaMensajeVacio() {
        ErrorRemoto error = new ErrorRemoto(CodigoError.INTERNO, "   ");

        assertEquals("Error interno del servidor.", error.mensaje());
    }

    static Stream<Arguments> excepcionesDeDominio() {
        return Stream.of(
                Arguments.of(new ValidacionException("validación"), CodigoError.VALIDACION),
                Arguments.of(new NoEncontradoException("no existe"), CodigoError.NO_ENCONTRADO),
                Arguments.of(new ConflictoException("conflicto"), CodigoError.CONFLICTO),
                Arguments.of(new AutenticacionException("credenciales"), CodigoError.AUTENTICACION),
                Arguments.of(new AccesoDatosException("base caída"), CodigoError.ACCESO_DATOS),
                Arguments.of(new ConfiguracionException("falta config"), CodigoError.CONFIGURACION),
                Arguments.of(new ServicioExternoException("IA caída"),
                        CodigoError.SERVICIO_EXTERNO));
    }
}
