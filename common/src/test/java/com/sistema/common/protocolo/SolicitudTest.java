package com.sistema.common.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.excepciones.ValidacionException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SolicitudTest extends PruebaProtocoloBase {

    @Test
    @DisplayName("Serializa y recupera una solicitud con todos sus parámetros")
    void serializaSolicitudCompleta() {
        Solicitud original = Solicitud.conId("corr-1", Operacion.CREAR_RESERVA,
                "222", "Reunión de equipo", LocalDate.of(2026, 3, 2),
                LocalTime.of(9, 0), LocalTime.of(11, 0), List.of("CAT-000001"));

        Solicitud recibida = idaYVuelta(original);

        assertEquals(original, recibida);
        assertEquals("corr-1", recibida.id());
        assertEquals(Operacion.CREAR_RESERVA, recibida.operacion());
        assertEquals(6, recibida.parametros().size());
        assertEquals(List.of("CAT-000001"), recibida.parametro(5));
    }

    @Test
    @DisplayName("Conserva un parámetro nulo, necesario al modificar sin nuevo id")
    void conservaParametrosNulos() {
        Solicitud original = Solicitud.conId("corr-2", Operacion.MODIFICAR_CATEGORIA,
                "CAT-000001", null, "Salas");

        Solicitud recibida = idaYVuelta(original);

        assertNull(recibida.parametro(1));
        assertEquals("Salas", recibida.parametro(2));
    }

    @Test
    @DisplayName("Cada solicitud creada recibe un identificador distinto")
    void generaIdentificadoresUnicos() {
        Set<String> ids = IntStream.range(0, 100)
                .mapToObj(i -> Solicitud.de(Operacion.LISTAR_CATEGORIAS).id())
                .collect(Collectors.toSet());

        assertEquals(100, ids.size());
        assertTrue(ids.stream().noneMatch(String::isBlank));
    }

    @Test
    @DisplayName("Valida identificador, operación, parámetros y aridad")
    void validaSolicitudMalFormada() {
        assertThrows(ValidacionException.class,
                () -> new Solicitud("  ", Operacion.INICIAR_SESION, List.of("222", "clave")));
        assertThrows(ValidacionException.class,
                () -> new Solicitud("corr", null, List.of()));
        assertThrows(ValidacionException.class,
                () -> new Solicitud("corr", Operacion.INICIAR_SESION, null));
        assertThrows(ValidacionException.class,
                () -> Solicitud.conId("corr", Operacion.INICIAR_SESION, "222"));
        assertThrows(ValidacionException.class,
                () -> Solicitud.conId("corr", Operacion.LISTAR_CATEGORIAS, "sobra"));
    }

    @Test
    @DisplayName("Rechaza índices de parámetro fuera de rango")
    void validaIndiceDeParametro() {
        Solicitud solicitud = Solicitud.de(Operacion.INICIAR_SESION, "222", "clave");

        assertThrows(ValidacionException.class, () -> solicitud.parametro(-1));
        assertThrows(ValidacionException.class, () -> solicitud.parametro(2));
    }
}
