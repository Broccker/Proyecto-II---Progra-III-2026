package com.sistema.backend.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Reserva;
import com.sistema.common.protocolo.Operacion;
import com.sistema.common.protocolo.Solicitud;
import com.sistema.common.servicio.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;

class DespachadorTest {

    private Service servicio;
    private Despachador despachador;

    @BeforeEach
    void preparar() {
        servicio = mock(Service.class);
        despachador = new Despachador(servicio);
    }

    @Test
    @DisplayName("Cada operación invoca el método de Service que le corresponde")
    void despachaTodasLasOperaciones() {
        Map<Operacion, Object[]> ejemplos = parametrosDeEjemplo();
        assertEquals(Operacion.values().length, ejemplos.size());

        for (Operacion operacion : Operacion.values()) {
            Object[] parametros = ejemplos.get(operacion);
            assertNotNull(parametros, "Falta un ejemplo de parámetros para " + operacion);

            clearInvocations(servicio);
            despachador.ejecutar(Solicitud.conId("corr-" + operacion, operacion, parametros));

            List<Invocation> invocaciones =
                    List.copyOf(mockingDetails(servicio).getInvocations());
            assertEquals(1, invocaciones.size(), "La operación " + operacion
                    + " debe invocar exactamente un método de Service.");
            Invocation invocacion = invocaciones.getFirst();
            assertEquals(operacion.metodo(), invocacion.getMethod().getName(),
                    "La operación " + operacion + " invocó otro método.");
            assertEquals(operacion.cantidadParametros(), invocacion.getArguments().length,
                    "La operación " + operacion + " pasó otra cantidad de argumentos.");
        }
    }

    @Test
    @DisplayName("Devuelve el resultado del servicio y admite el nuevo id nulo")
    void devuelveElResultadoYConservaNulosOpcionales() {
        Reserva reserva = DatosPrueba.reserva();
        when(servicio.crearReserva(anyString(), anyString(), any(LocalDate.class),
                any(LocalTime.class), any(LocalTime.class), anyList())).thenReturn(reserva);

        Object resultado = despachador.ejecutar(Solicitud.de(Operacion.CREAR_RESERVA,
                DatosPrueba.ID_FUNCIONARIO, DatosPrueba.ACTIVIDAD, DatosPrueba.FECHA,
                DatosPrueba.HORA_INICIO, DatosPrueba.HORA_FIN, List.of(DatosPrueba.ID_CATEGORIA)));
        assertSame(reserva, resultado);

        despachador.ejecutar(Solicitud.de(Operacion.MODIFICAR_CATEGORIA,
                DatosPrueba.ID_CATEGORIA, null, "Salas"));
        verify(servicio).modificarCategoria(DatosPrueba.ID_CATEGORIA, null, "Salas");
    }

    @Test
    @DisplayName("Propaga los errores de dominio para que el manejador los traduzca")
    void propagaErroresDeDominio() {
        when(servicio.listarCategorias())
                .thenThrow(new ConflictoException("No hay categorías disponibles."));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> despachador.ejecutar(Solicitud.de(Operacion.LISTAR_CATEGORIAS)));

        assertEquals("No hay categorías disponibles.", error.getMessage());
    }

    @Test
    @DisplayName("Rechaza parámetros con un tipo distinto al que espera el método")
    void rechazaParametrosConTipoIncorrecto() {
        assertThrows(ValidacionException.class, () -> despachador.ejecutar(
                Solicitud.conId("c1", Operacion.CREAR_RESERVA, DatosPrueba.ID_FUNCIONARIO,
                        DatosPrueba.ACTIVIDAD, "2026-11-02", DatosPrueba.HORA_INICIO,
                        DatosPrueba.HORA_FIN, List.of(DatosPrueba.ID_CATEGORIA))));
        assertThrows(ValidacionException.class, () -> despachador.ejecutar(
                Solicitud.conId("c2", Operacion.CONSTRUIR_MATRIZ_ACTIVIDADES, "ayer")));
        assertThrows(ValidacionException.class, () -> despachador.ejecutar(
                Solicitud.conId("c3", Operacion.FILTRAR_RECURSOS,
                        DatosPrueba.ID_CATEGORIA, 7)));
        assertThrows(ValidacionException.class, () -> despachador.ejecutar(
                Solicitud.conId("c4", Operacion.CREAR_RESERVA, DatosPrueba.ID_FUNCIONARIO,
                        DatosPrueba.ACTIVIDAD, DatosPrueba.FECHA, DatosPrueba.HORA_INICIO,
                        DatosPrueba.HORA_FIN, List.of(7))));
        assertThrows(ValidacionException.class, () -> despachador.ejecutar(
                Solicitud.conId("c5", Operacion.MODIFICAR_CATEGORIA,
                        DatosPrueba.ID_CATEGORIA, 42, "Salas")));
    }

    @Test
    @DisplayName("Exige una solicitud")
    void exigeSolicitud() {
        assertThrows(NullPointerException.class, () -> despachador.ejecutar(null));
    }

    private static Map<Operacion, Object[]> parametrosDeEjemplo() {
        Map<Operacion, Object[]> ejemplos = new LinkedHashMap<>();

        ejemplos.put(Operacion.INICIAR_SESION, new Object[]{"222", "clave"});
        ejemplos.put(Operacion.CAMBIAR_CLAVE,
                new Object[]{"222", "clave", "nueva", "nueva"});

        ejemplos.put(Operacion.ALTA_FUNCIONARIO, new Object[]{"555", "Carlos", "88880000"});
        ejemplos.put(Operacion.MODIFICAR_FUNCIONARIO,
                new Object[]{"555", "Carlos Rojas", "88880001"});
        ejemplos.put(Operacion.ELIMINAR_FUNCIONARIO, new Object[]{"555"});
        ejemplos.put(Operacion.LISTAR_FUNCIONARIOS, new Object[]{});
        ejemplos.put(Operacion.BUSCAR_FUNCIONARIOS, new Object[]{"Carlos"});

        ejemplos.put(Operacion.ALTA_CATEGORIA, new Object[]{"Salas"});
        ejemplos.put(Operacion.MODIFICAR_CATEGORIA,
                new Object[]{DatosPrueba.ID_CATEGORIA, null, "Salas"});
        ejemplos.put(Operacion.ELIMINAR_CATEGORIA, new Object[]{DatosPrueba.ID_CATEGORIA});
        ejemplos.put(Operacion.LISTAR_CATEGORIAS, new Object[]{});
        ejemplos.put(Operacion.BUSCAR_CATEGORIAS, new Object[]{"Proy"});
        ejemplos.put(Operacion.EXISTE_DESCRIPCION_CATEGORIA,
                new Object[]{"Proyectores"});

        ejemplos.put(Operacion.ALTA_RECURSO,
                new Object[]{"1002", DatosPrueba.ID_CATEGORIA, "Proyector"});
        ejemplos.put(Operacion.MODIFICAR_RECURSO,
                new Object[]{"1002", null, DatosPrueba.ID_CATEGORIA, "Proyector"});
        ejemplos.put(Operacion.ELIMINAR_RECURSO, new Object[]{"1002"});
        ejemplos.put(Operacion.LISTAR_RECURSOS, new Object[]{});
        ejemplos.put(Operacion.BUSCAR_RECURSOS, new Object[]{"Proyector"});
        ejemplos.put(Operacion.FILTRAR_RECURSOS,
                new Object[]{DatosPrueba.ID_CATEGORIA, "Proyector"});

        ejemplos.put(Operacion.CREAR_RESERVA,
                new Object[]{DatosPrueba.ID_FUNCIONARIO, DatosPrueba.ACTIVIDAD,
                        DatosPrueba.FECHA, DatosPrueba.HORA_INICIO, DatosPrueba.HORA_FIN,
                        List.of(DatosPrueba.ID_CATEGORIA)});
        ejemplos.put(Operacion.CANCELAR_RESERVA,
                new Object[]{DatosPrueba.ID_RESERVA, DatosPrueba.ID_FUNCIONARIO});
        ejemplos.put(Operacion.LISTAR_RESERVAS_DE, new Object[]{DatosPrueba.ID_FUNCIONARIO});
        ejemplos.put(Operacion.LISTAR_TODAS_LAS_RESERVAS, new Object[]{});
        ejemplos.put(Operacion.LISTAR_RESERVAS_ACTIVAS_ENTRE,
                new Object[]{DatosPrueba.FECHA, DatosPrueba.FECHA.plusDays(7)});

        ejemplos.put(Operacion.CONSTRUIR_MATRIZ_ACTIVIDADES,
                new Object[]{DatosPrueba.FECHA});
        ejemplos.put(Operacion.CONSTRUIR_MATRIZ_CALENDARIZACION,
                new Object[]{DatosPrueba.FECHA, DatosPrueba.ID_CATEGORIA});
        ejemplos.put(Operacion.CATEGORIAS_RESERVADAS,
                new Object[]{DatosPrueba.FECHA, DatosPrueba.FECHA.plusDays(7)});
        ejemplos.put(Operacion.ACTIVIDADES_POR_SEMANA,
                new Object[]{DatosPrueba.FECHA, DatosPrueba.FECHA.plusDays(7)});
        ejemplos.put(Operacion.EXTRAER_DATOS_RESERVA,
                new Object[]{"Reservar el proyector el lunes de 9 a 11"});

        return ejemplos;
    }
}
