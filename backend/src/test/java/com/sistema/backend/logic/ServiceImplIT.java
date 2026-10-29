package com.sistema.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.backend.data.CategoriaRecursoDAO;
import com.sistema.backend.data.PruebaBDBase;
import com.sistema.backend.data.RecursoDAO;
import com.sistema.backend.data.ReservaDAO;
import com.sistema.backend.data.UsuarioDAO;
import com.sistema.backend.logic.ia.ClienteIA;
import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.excepciones.AutenticacionException;
import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.ConfiguracionException;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.EstadoReserva;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Pruebas de integración de la capa lógica contra MySQL.
 *
 * <p>Usan la misma base de prueba que los DAO. Si MySQL no está activo se
 * omiten con el mensaje de {@link PruebaBDBase}.</p>
 */
class ServiceImplIT extends PruebaBDBase {

    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-03-02T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate HOY = LocalDate.of(2026, 3, 2);

    private ServiceImpl servicio;
    private Funcionario ana;
    private CategoriaRecurso proyectores;
    private Recurso proyector;

    @BeforeEach
    void prepararServicio() {
        servicio = new ServiceImpl(new UsuarioDAO(fuente()),
                new CategoriaRecursoDAO(fuente()), new RecursoDAO(fuente()),
                new ReservaDAO(fuente()), new ClienteIA("", "", ""), RELOJ);
        ana = crearFuncionario("222", "Ana Pérez", "88881111");
        proyectores = crearCategoria("CAT-000001", "Proyectores");
        proyector = crearRecurso("1001", proyectores, "Proyector Epson");
    }

    @Test
    @DisplayName("Autentica contra el hash BCrypt y cambia la clave")
    void autenticaYCambiaClave() {
        assertInstanceOf(Funcionario.class, servicio.iniciarSesion("222", "clave"));
        assertThrows(AutenticacionException.class,
                () -> servicio.iniciarSesion("222", "incorrecta"));

        servicio.cambiarClave("222", "clave", "nueva-clave", "nueva-clave");

        assertThrows(AutenticacionException.class,
                () -> servicio.iniciarSesion("222", "clave"));
        assertEquals("222", servicio.iniciarSesion("222", "nueva-clave").getId());
    }

    @Test
    @DisplayName("Da de alta un funcionario que puede entrar con su identificador")
    void altaFuncionarioConClaveInicial() {
        servicio.altaFuncionario("555", "Carlos Mora", "77778888");

        Funcionario carlos = (Funcionario) servicio.iniciarSesion("555", "555");
        assertEquals("Carlos Mora", carlos.getNombre());
        assertThrows(ConflictoException.class,
                () -> servicio.altaFuncionario("555", "Otro", "77779999"));
    }

    @Test
    @DisplayName("Crea una reserva y rechaza el traslape del único recurso")
    void creaReservaYRechazaTraslape() {
        Reserva reserva = servicio.crearReserva("222", "Reunión de equipo", HOY,
                LocalTime.of(9, 0), LocalTime.of(11, 0), List.of("CAT-000001"));

        assertEquals("RES-000001", reserva.getId());
        assertEquals("1001", reserva.getRecursos().getFirst().getId());
        assertEquals(1, servicio.listarReservasDe("222").size());

        assertThrows(ConflictoException.class, () -> servicio.crearReserva("222",
                "Otra actividad", HOY, LocalTime.of(10, 0), LocalTime.of(12, 0),
                List.of("CAT-000001")));
        assertEquals(1, contar("reservas"));
    }

    @Test
    @DisplayName("Cancela una reserva futura solo para su dueño")
    void cancelaReservaDelDueno() {
        crearFuncionario("333", "Luis Rojas", "88882222");
        Reserva reserva = servicio.crearReserva("222", "Reunión", HOY,
                LocalTime.of(9, 0), LocalTime.of(11, 0), List.of("CAT-000001"));

        assertThrows(ConflictoException.class,
                () -> servicio.cancelarReserva(reserva.getId(), "333"));

        servicio.cancelarReserva(reserva.getId(), "222");

        assertEquals(EstadoReserva.CANCELADA,
                new ReservaDAO(fuente()).buscarPorId(reserva.getId()).orElseThrow().getEstado());
        assertTrue(servicio.listarReservasDe("222").isEmpty());
        assertThrows(ConflictoException.class,
                () -> servicio.cancelarReserva(reserva.getId(), "222"));
    }

    @Test
    @DisplayName("Calcula estadísticas con las reservas activas")
    void calculaEstadisticas() {
        servicio.crearReserva("222", "Semana 1", HOY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of("CAT-000001"));
        servicio.crearReserva("222", "Semana 2", HOY.plusDays(7),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of("CAT-000001"));

        List<ConteoCategoria> categorias =
                servicio.categoriasReservadas(HOY, HOY.plusDays(14));
        assertEquals(1, categorias.size());
        assertEquals(2, categorias.getFirst().cantidad());
        assertEquals(2, servicio.actividadesPorSemana(HOY, HOY.plusDays(14)).size());
    }

    @Test
    @DisplayName("Construye las matrices de actividades y calendarización")
    void construyeMatrices() {
        servicio.crearReserva("222", "Reunión", HOY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of("CAT-000001"));

        var actividades = servicio.construirMatrizActividades(HOY);
        assertTrue(actividades.aTexto()[9][0].contains("Reunión"));

        var calendarizacion = servicio.construirMatrizCalendarizacion(HOY, "CAT-000001");
        assertTrue(calendarizacion.aTexto()[9][0].contains("Reunión"));
        assertEquals("1001", calendarizacion.recursos().getFirst().getId());
    }

    @Test
    @DisplayName("No permite borrar una categoría con recursos ni un recurso reservado")
    void protegeCatalogosEnUso() {
        assertThrows(ConflictoException.class,
                () -> servicio.eliminarCategoria("CAT-000001"));
        assertEquals(1, servicio.listarRecursos().size());

        servicio.crearReserva("222", "Reunión", HOY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of("CAT-000001"));
        assertThrows(ConflictoException.class,
                () -> servicio.eliminarRecurso("1001"));
    }

    @Test
    @DisplayName("No permite reservar en una fecha pasada")
    void rechazaFechaPasada() {
        assertThrows(com.sistema.common.excepciones.ValidacionException.class,
                () -> servicio.crearReserva("222", "Reunión", HOY.minusDays(1),
                        LocalTime.of(9, 0), LocalTime.of(10, 0), List.of("CAT-000001")));
    }

    @Test
    @Timeout(20)
    @DisplayName("Dos peticiones simultáneas al mismo recurso crean una sola reserva")
    void dosPeticionesSimultaneasNoDuplicanLaReserva() throws Exception {
        crearFuncionario("333", "Luis Rojas", "88882222");
        ServiceImpl otroBackend = nuevoServicio();
        CyclicBarrier barrera = new CyclicBarrier(2);
        ExecutorService ejecutor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            Future<Boolean> primera = ejecutor.submit(
                    () -> intentarReserva(servicio, "222", barrera));
            Future<Boolean> segunda = ejecutor.submit(
                    () -> intentarReserva(otroBackend, "333", barrera));

            int exitos = (primera.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (segunda.get(10, TimeUnit.SECONDS) ? 1 : 0);

            assertEquals(1, exitos,
                    "Solo una de las dos peticiones debe crear la reserva.");
        } finally {
            ejecutor.shutdownNow();
        }
        assertEquals(1, contar("reservas"));
        assertEquals(1, contar("reserva_recursos"));
    }

    @Test
    @DisplayName("La IA queda deshabilitada si no hay URL configurada")
    void iaSinConfiguracion() {
        assertThrows(ConfiguracionException.class,
                () -> servicio.extraerDatosReserva("Reserve una sala mañana"));
    }

    @Test
    @DisplayName("Modifica categorías y recursos propagando los identificadores")
    void modificaCatalogos() {
        servicio.modificarCategoria("CAT-000001", "CAT-000010", "Proyectores y pantallas");
        servicio.modificarRecurso("1001", "1010", "CAT-000010", "Proyector Epson EB-X51");

        assertEquals("CAT-000010", servicio.listarCategorias().getFirst().getId());
        Recurso actualizado = servicio.listarRecursos().getFirst();
        assertEquals("1010", actualizado.getId());
        assertEquals("CAT-000010", actualizado.getCategoria().getId());
    }

    private ServiceImpl nuevoServicio() {
        return new ServiceImpl(new UsuarioDAO(fuente()), new CategoriaRecursoDAO(fuente()),
                new RecursoDAO(fuente()), new ReservaDAO(fuente()),
                new ClienteIA("", "", ""), RELOJ);
    }

    private static boolean intentarReserva(ServiceImpl servicio, String funcionarioId,
                                           CyclicBarrier barrera) throws Exception {
        barrera.await(5, TimeUnit.SECONDS);
        try {
            servicio.crearReserva(funcionarioId, "Reunión simultánea", HOY,
                    LocalTime.of(9, 0), LocalTime.of(11, 0), List.of("CAT-000001"));
            return true;
        } catch (ConflictoException e) {
            return false;
        }
    }
}
