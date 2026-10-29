package com.sistema.backend.logic;

import com.sistema.backend.data.CategoriaRecursoDAO;
import com.sistema.backend.data.RecursoDAO;
import com.sistema.backend.data.ReservaDAO;
import com.sistema.backend.data.UsuarioDAO;
import com.sistema.backend.logic.ia.ClienteIA;
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
import org.junit.jupiter.api.BeforeEach;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;

/**
 * Base de las pruebas unitarias de {@link ServiceImpl}.
 *
 * <p>Los DAO y el cliente de IA se simulan con Mockito. El reloj se fija en
 * un lunes a las 10:00 (UTC) para que las reglas de reservas futuras y las
 * matrices semanales sean deterministas.</p>
 */
abstract class PruebaServicioBase {

    protected static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-03-02T10:00:00Z"), ZoneOffset.UTC);
    protected static final LocalDate HOY = LocalDate.of(2026, 3, 2);
    protected static final String HASH_CLAVE = BCrypt.hashpw("clave", BCrypt.gensalt(4));

    @Mock
    protected UsuarioDAO usuarioDAO;
    @Mock
    protected CategoriaRecursoDAO categoriaDAO;
    @Mock
    protected RecursoDAO recursoDAO;
    @Mock
    protected ReservaDAO reservaDAO;
    @Mock
    protected ClienteIA clienteIA;

    protected ServiceImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ServiceImpl(usuarioDAO, categoriaDAO, recursoDAO, reservaDAO,
                clienteIA, RELOJ);
    }

    protected static Funcionario funcionario(String id) {
        return new Funcionario(id, "Ana Pérez", "88881111");
    }

    protected static CategoriaRecurso categoria(String id, String descripcion) {
        return new CategoriaRecurso(id, descripcion);
    }

    protected static Recurso recurso(String id, CategoriaRecurso categoria, String descripcion) {
        return new Recurso(id, categoria, descripcion);
    }

    protected static Reserva reserva(String id, Funcionario funcionario, LocalDate fecha,
                                     String horaInicio, String horaFin, EstadoReserva estado,
                                     Recurso... recursos) {
        return new Reserva(id, funcionario, "Actividad " + id, fecha,
                LocalTime.parse(horaInicio), LocalTime.parse(horaFin),
                List.of(recursos), estado);
    }
}
