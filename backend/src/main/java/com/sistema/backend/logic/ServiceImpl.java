package com.sistema.backend.logic;

import com.sistema.backend.config.ConfiguracionBackend;
import com.sistema.backend.data.CategoriaRecursoDAO;
import com.sistema.backend.data.FuenteDatos;
import com.sistema.backend.data.RecursoDAO;
import com.sistema.backend.data.ReservaDAO;
import com.sistema.backend.data.UsuarioDAO;
import com.sistema.backend.logic.ia.ClienteIA;
import com.sistema.backend.logic.ia.MapeoCategorias;
import com.sistema.backend.logic.ia.PropuestaIA;
import com.sistema.common.dto.CeldaCalendario;
import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import com.sistema.common.dto.DatosReservaSugerida;
import com.sistema.common.dto.MatrizActividades;
import com.sistema.common.dto.MatrizCalendarizacion;
import com.sistema.common.excepciones.AutenticacionException;
import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.EstadoReserva;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import com.sistema.common.modelo.Usuario;
import com.sistema.common.servicio.Service;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Implementación de la capa lógica del backend.
 *
 * <p>Todas las reglas del Proyecto #1 se validan aquí, del lado servidor,
 * aunque el frontend también valide: credenciales BCrypt, unicidad de
 * identificadores y descripciones, coherencia de fechas y horas, pertenencia
 * de la reserva al funcionario, disponibilidad de recursos y límites de las
 * columnas de MySQL.</p>
 *
 * <p>El recurso elegido para cada categoría se comprueba dos veces: aquí para
 * dar un mensaje descriptivo y en {@link ReservaDAO#insertar} dentro de una
 * transacción con {@code SELECT ... FOR UPDATE}, que es la garantía real
 * frente a dos peticiones simultáneas.</p>
 */
public final class ServiceImpl implements Service {

    private static final int MAX_ID = 20;
    private static final int MAX_NOMBRE = 100;
    private static final int MAX_TELEFONO = 20;
    private static final int MAX_DESCRIPCION_CATEGORIA = 100;
    private static final int MAX_DESCRIPCION_RECURSO = 150;
    private static final int MAX_ACTIVIDAD = 150;
    private static final int MAX_FRASE_IA = 1000;

    private static final String[] NOMBRES_DIAS = {
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    };

    private final UsuarioDAO usuarioDAO;
    private final CategoriaRecursoDAO categoriaDAO;
    private final RecursoDAO recursoDAO;
    private final ReservaDAO reservaDAO;
    private final ClienteIA clienteIA;
    private final Clock reloj;

    /**
     * Serie de identificadores y creación de reservas. Un solo backend atiende
     * a todos los clientes, así que este bloqueo evita que dos peticiones
     * simultáneas calculen el mismo {@code CAT-...} o {@code RES-...}. La
     * transacción del DAO sigue siendo la garantía frente a otras instancias.
     */
    private final Object bloqueoAltas = new Object();

    public ServiceImpl(FuenteDatos fuente, ConfiguracionBackend configuracion) {
        this(new UsuarioDAO(fuente), new CategoriaRecursoDAO(fuente), new RecursoDAO(fuente),
                new ReservaDAO(fuente),
                new ClienteIA(configuracion.urlIA(), configuracion.claveIA(),
                        configuracion.modeloIA()),
                Clock.systemDefaultZone());
    }

    /**
     * Constructor para pruebas: permite inyectar DAOs simulados, un cliente de
     * IA falso y un reloj fijo para las reglas de fechas.
     */
    public ServiceImpl(UsuarioDAO usuarioDAO, CategoriaRecursoDAO categoriaDAO,
                       RecursoDAO recursoDAO, ReservaDAO reservaDAO,
                       ClienteIA clienteIA, Clock reloj) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "El DAO de usuarios es obligatorio.");
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO,
                "El DAO de categorías es obligatorio.");
        this.recursoDAO = Objects.requireNonNull(recursoDAO, "El DAO de recursos es obligatorio.");
        this.reservaDAO = Objects.requireNonNull(reservaDAO, "El DAO de reservas es obligatorio.");
        this.clienteIA = Objects.requireNonNull(clienteIA, "El cliente de IA es obligatorio.");
        this.reloj = Objects.requireNonNull(reloj, "El reloj es obligatorio.");
    }

    // ------------------------------------------------------------------
    // Sesión
    // ------------------------------------------------------------------

    @Override
    public Usuario iniciarSesion(String id, String clave) {
        String identificador = Validaciones.texto(id, "El identificador", MAX_ID);
        Validaciones.clave(clave, "La clave");

        Optional<Usuario> usuario = usuarioDAO.buscarPorId(identificador);
        Optional<String> hash = usuarioDAO.buscarClaveHash(identificador);
        if (usuario.isEmpty() || hash.isEmpty() || !coincide(clave, hash.get())) {
            // El mismo mensaje evita revelar si el identificador existe.
            throw new AutenticacionException("Identificador o clave incorrectos.");
        }
        return usuario.get();
    }

    @Override
    public void cambiarClave(String id, String claveActual, String nuevaClave,
                             String confirmacion) {
        String identificador = Validaciones.texto(id, "El identificador", MAX_ID);
        Validaciones.clave(claveActual, "La clave actual");
        Validaciones.clave(nuevaClave, "La clave nueva");
        if (!nuevaClave.equals(confirmacion)) {
            throw new ValidacionException("La confirmación no coincide con la clave nueva.");
        }
        if (nuevaClave.equals(claveActual)) {
            throw new ValidacionException("La clave nueva debe ser distinta de la actual.");
        }

        String hash = usuarioDAO.buscarClaveHash(identificador)
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe el usuario con identificador " + identificador + "."));
        if (!coincide(claveActual, hash)) {
            throw new AutenticacionException("La clave actual es incorrecta.");
        }
        usuarioDAO.actualizarClave(identificador, BCrypt.hashpw(nuevaClave, BCrypt.gensalt()));
    }

    // ------------------------------------------------------------------
    // Funcionarios
    // ------------------------------------------------------------------

    /**
     * Da de alta un funcionario. La clave inicial es su propio identificador,
     * como en el Proyecto #1; se guarda siempre como hash BCrypt.
     */
    @Override
    public void altaFuncionario(String id, String nombre, String telefono) {
        String identificador = Validaciones.texto(id, "El identificador del funcionario", MAX_ID);
        Funcionario funcionario = new Funcionario(identificador, nombre, telefono);
        Validaciones.longitud(funcionario.getNombre(), MAX_NOMBRE, "El nombre");
        Validaciones.longitud(funcionario.getTelefono(), MAX_TELEFONO, "El teléfono");

        synchronized (bloqueoAltas) {
            if (usuarioDAO.existeId(identificador)) {
                throw new ConflictoException(
                        "Ya existe un usuario con el identificador " + identificador + ".");
            }
            usuarioDAO.insertar(funcionario,
                    BCrypt.hashpw(identificador, BCrypt.gensalt()));
        }
    }

    @Override
    public void modificarFuncionario(String id, String nombre, String telefono) {
        Funcionario existente = buscarFuncionario(id);
        Funcionario modificado = new Funcionario(existente.getId(), nombre, telefono);
        Validaciones.longitud(modificado.getNombre(), MAX_NOMBRE, "El nombre");
        Validaciones.longitud(modificado.getTelefono(), MAX_TELEFONO, "El teléfono");
        usuarioDAO.actualizarFuncionario(modificado);
    }

    @Override
    public void eliminarFuncionario(String id) {
        Funcionario existente = buscarFuncionario(id);
        // Se conserva el historial: tanto las reservas activas como las
        // canceladas impiden borrar al funcionario.
        if (!reservaDAO.listarPorFuncionario(existente.getId()).isEmpty()) {
            throw new ConflictoException(
                    "No se puede eliminar al funcionario porque tiene reservas registradas.");
        }
        usuarioDAO.eliminar(existente.getId());
    }

    @Override
    public List<Funcionario> listarFuncionarios() {
        return usuarioDAO.listarFuncionarios();
    }

    @Override
    public List<Funcionario> buscarFuncionarios(String criterio) {
        return usuarioDAO.buscarFuncionarios(criterio);
    }

    // ------------------------------------------------------------------
    // Categorías de recursos
    // ------------------------------------------------------------------

    @Override
    public CategoriaRecurso altaCategoria(String descripcion) {
        String texto = Validaciones.texto(descripcion,
                "La descripción de la categoría", MAX_DESCRIPCION_CATEGORIA);
        synchronized (bloqueoAltas) {
            if (categoriaDAO.existeDescripcion(texto)) {
                throw new ConflictoException("Ya existe una categoría con esa descripción.");
            }
            CategoriaRecurso nueva = new CategoriaRecurso(categoriaDAO.siguienteId(), texto);
            categoriaDAO.insertar(nueva);
            return nueva;
        }
    }

    @Override
    public void modificarCategoria(String id, String nuevoId, String descripcion) {
        String actual = Validaciones.texto(id, "El identificador de la categoría", MAX_ID);
        String texto = Validaciones.texto(descripcion,
                "La descripción de la categoría", MAX_DESCRIPCION_CATEGORIA);
        String destino = nuevoId == null || nuevoId.isBlank()
                ? actual
                : Validaciones.texto(nuevoId, "El identificador de la categoría", MAX_ID);

        categoriaDAO.buscarPorId(actual).orElseThrow(() -> new NoEncontradoException(
                "No existe la categoría con identificador " + actual + "."));

        synchronized (bloqueoAltas) {
            if (!destino.equals(actual) && categoriaDAO.existeId(destino)) {
                throw new ConflictoException(
                        "Ya existe una categoría con el identificador " + destino + ".");
            }
            if (categoriaDAO.existeDescripcion(texto, actual)) {
                throw new ConflictoException("Ya existe una categoría con esa descripción.");
            }
            // ON UPDATE CASCADE propaga el nuevo id a los recursos.
            categoriaDAO.actualizar(actual, new CategoriaRecurso(destino, texto));
        }
    }

    @Override
    public void eliminarCategoria(String id) {
        String actual = Validaciones.texto(id, "El identificador de la categoría", MAX_ID);
        categoriaDAO.buscarPorId(actual).orElseThrow(() -> new NoEncontradoException(
                "No existe la categoría con identificador " + actual + "."));
        if (categoriaDAO.contarRecursos(actual) > 0) {
            throw new ConflictoException(
                    "No se puede eliminar la categoría porque tiene recursos asociados.");
        }
        categoriaDAO.eliminar(actual);
    }

    @Override
    public List<CategoriaRecurso> listarCategorias() {
        return categoriaDAO.listarTodas();
    }

    @Override
    public List<CategoriaRecurso> buscarCategorias(String criterio) {
        return categoriaDAO.buscar(criterio);
    }

    @Override
    public boolean existeDescripcionCategoria(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return false;
        }
        return categoriaDAO.existeDescripcion(descripcion);
    }

    // ------------------------------------------------------------------
    // Recursos
    // ------------------------------------------------------------------

    @Override
    public void altaRecurso(String id, String categoriaId, String descripcion) {
        String identificador = Validaciones.texto(id, "El identificador del recurso", MAX_ID);
        String texto = Validaciones.texto(descripcion,
                "La descripción del recurso", MAX_DESCRIPCION_RECURSO);
        CategoriaRecurso categoria = buscarCategoria(categoriaId);

        synchronized (bloqueoAltas) {
            if (recursoDAO.existeId(identificador)) {
                throw new ConflictoException(
                        "Ya existe un recurso con el identificador " + identificador + ".");
            }
            if (recursoDAO.existeDescripcionEnCategoria(
                    categoria.getId(), texto, null)) {
                throw new ConflictoException(
                        "Ya existe un recurso con esa descripción en la categoría "
                                + categoria.getDescripcion() + ".");
            }
            recursoDAO.insertar(new Recurso(identificador, categoria, texto));
        }
    }

    @Override
    public void modificarRecurso(String id, String nuevoId, String categoriaId,
                                 String descripcion) {
        String actual = Validaciones.texto(id, "El identificador del recurso", MAX_ID);
        String texto = Validaciones.texto(descripcion,
                "La descripción del recurso", MAX_DESCRIPCION_RECURSO);
        String destino = nuevoId == null || nuevoId.isBlank()
                ? actual
                : Validaciones.texto(nuevoId, "El identificador del recurso", MAX_ID);
        CategoriaRecurso categoria = buscarCategoria(categoriaId);

        recursoDAO.buscarPorId(actual).orElseThrow(() -> new NoEncontradoException(
                "No existe el recurso con identificador " + actual + "."));

        synchronized (bloqueoAltas) {
            if (!destino.equals(actual) && recursoDAO.existeId(destino)) {
                throw new ConflictoException(
                        "Ya existe un recurso con el identificador " + destino + ".");
            }
            if (recursoDAO.existeDescripcionEnCategoria(
                    categoria.getId(), texto, actual)) {
                throw new ConflictoException(
                        "Ya existe un recurso con esa descripción en la categoría "
                                + categoria.getDescripcion() + ".");
            }
            recursoDAO.actualizar(actual, new Recurso(destino, categoria, texto));
        }
    }

    @Override
    public void eliminarRecurso(String id) {
        String actual = Validaciones.texto(id, "El identificador del recurso", MAX_ID);
        recursoDAO.buscarPorId(actual).orElseThrow(() -> new NoEncontradoException(
                "No existe el recurso con identificador " + actual + "."));
        if (reservaDAO.existeRecursoReservado(actual)) {
            throw new ConflictoException(
                    "No se puede eliminar el recurso porque está incluido en reservas.");
        }
        recursoDAO.eliminar(actual);
    }

    @Override
    public List<Recurso> listarRecursos() {
        return recursoDAO.listarTodos();
    }

    @Override
    public List<Recurso> buscarRecursos(String criterio) {
        return recursoDAO.buscar(criterio);
    }

    @Override
    public List<Recurso> filtrarRecursos(String categoriaId, String criterio) {
        return recursoDAO.filtrar(categoriaId, criterio);
    }

    // ------------------------------------------------------------------
    // Reservas
    // ------------------------------------------------------------------

    @Override
    public Reserva crearReserva(String funcionarioId, String actividad, LocalDate fecha,
                                LocalTime horaInicio, LocalTime horaFin,
                                List<String> idsCategorias) {
        Funcionario funcionario = buscarFuncionario(funcionarioId);
        String titulo = Validaciones.texto(actividad, "La actividad", MAX_ACTIVIDAD);
        validarHorarioReserva(fecha, horaInicio, horaFin);

        List<String> ids = idsCategorias == null ? List.of() : idsCategorias.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            throw new ValidacionException("Debe seleccionar al menos una categoría.");
        }
        Map<String, CategoriaRecurso> categorias = new LinkedHashMap<>();
        for (String id : ids) {
            categorias.put(id, buscarCategoria(id));
        }

        synchronized (bloqueoAltas) {
            Set<String> ocupados = reservaDAO.recursosOcupados(fecha, horaInicio, horaFin);
            List<Recurso> seleccionados = new ArrayList<>();
            List<String> sinDisponibilidad = new ArrayList<>();
            for (CategoriaRecurso categoria : categorias.values()) {
                Recurso libre = recursoDAO.listarPorCategoria(categoria.getId()).stream()
                        .filter(recurso -> !ocupados.contains(recurso.getId()))
                        .findFirst()
                        .orElse(null);
                if (libre == null) {
                    sinDisponibilidad.add(categoria.getDescripcion());
                } else {
                    seleccionados.add(libre);
                }
            }
            if (!sinDisponibilidad.isEmpty()) {
                throw new ConflictoException("No hay recursos disponibles en: "
                        + String.join(", ", sinDisponibilidad) + ".");
            }
            Reserva reserva = new Reserva(reservaDAO.siguienteId(), funcionario, titulo, fecha,
                    horaInicio, horaFin, seleccionados, EstadoReserva.ACTIVA);
            reservaDAO.insertar(reserva);
            return reserva;
        }
    }

    @Override
    public void cancelarReserva(String idReserva, String funcionarioId) {
        String identificador = Validaciones.texto(idReserva,
                "El identificador de la reserva", MAX_ID);
        Funcionario funcionario = buscarFuncionario(funcionarioId);

        Reserva reserva = reservaDAO.buscarPorId(identificador)
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe la reserva con identificador " + identificador + "."));
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ConflictoException("La reserva ya está cancelada.");
        }
        if (!reserva.getFuncionario().getId().equals(funcionario.getId())) {
            throw new ConflictoException("La reserva no pertenece al funcionario actual.");
        }
        if (!LocalDateTime.of(reserva.getFecha(), reserva.getHoraFin())
                .isAfter(LocalDateTime.now(reloj))) {
            throw new ConflictoException("Solo se pueden cancelar reservas futuras.");
        }
        reservaDAO.cambiarEstado(identificador, EstadoReserva.CANCELADA);
    }

    @Override
    public List<Reserva> listarReservasDe(String funcionarioId) {
        Funcionario funcionario = buscarFuncionario(funcionarioId);
        return reservaDAO.listarPorFuncionario(funcionario.getId()).stream()
                .filter(reserva -> reserva.getEstado() != EstadoReserva.CANCELADA)
                .toList();
    }

    @Override
    public List<Reserva> listarTodasLasReservas() {
        return reservaDAO.listarTodas();
    }

    @Override
    public List<Reserva> listarReservasActivasEntre(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return reservaDAO.listarActivasEntre(desde, hasta);
    }

    // ------------------------------------------------------------------
    // Actividades y calendarización
    // ------------------------------------------------------------------

    @Override
    public MatrizActividades construirMatrizActividades(LocalDate fecha) {
        if (fecha == null) {
            throw new ValidacionException("Debe indicar una fecha de referencia.");
        }
        LocalDate lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate domingo = lunes.plusDays(6);
        List<Reserva> activas = reservaDAO.listarActivasEntre(lunes, domingo);

        List<LocalTime> horas = horasDelDia();
        List<List<CeldaCalendario>> filas = new ArrayList<>(horas.size());
        for (LocalTime hora : horas) {
            List<CeldaCalendario> fila = new ArrayList<>(NOMBRES_DIAS.length);
            for (int dia = 0; dia < NOMBRES_DIAS.length; dia++) {
                LocalDate fechaDia = lunes.plusDays(dia);
                String texto = activas.stream()
                        .filter(reserva -> reserva.getFecha().equals(fechaDia))
                        .filter(reserva -> seSolapa(reserva.getHoraInicio(),
                                reserva.getHoraFin(), hora))
                        .map(reserva -> reserva.getActividad() + " ("
                                + reserva.getFuncionario().getNombre() + ")")
                        .reduce((primero, segundo) -> primero + "; " + segundo)
                        .orElse("");
                fila.add(new CeldaCalendario(texto, !texto.isEmpty()));
            }
            filas.add(fila);
        }
        return new MatrizActividades(lunes, horas, List.of(NOMBRES_DIAS), filas);
    }

    @Override
    public MatrizCalendarizacion construirMatrizCalendarizacion(LocalDate fecha,
                                                                String categoriaId) {
        if (fecha == null) {
            throw new ValidacionException("Debe indicar la fecha.");
        }
        CategoriaRecurso categoria = buscarCategoria(categoriaId);
        List<Recurso> recursos = recursoDAO.listarPorCategoria(categoria.getId());
        if (recursos.isEmpty()) {
            throw new ValidacionException("La categoría '" + categoria.getDescripcion()
                    + "' no tiene recursos registrados.");
        }
        List<Reserva> activas = reservaDAO.listarActivasEnFecha(fecha);

        List<LocalTime> horas = horasDelDia();
        List<List<CeldaCalendario>> filas = new ArrayList<>(horas.size());
        for (LocalTime hora : horas) {
            List<CeldaCalendario> fila = new ArrayList<>(recursos.size());
            for (Recurso recurso : recursos) {
                Reserva reserva = activas.stream()
                        .filter(candidata -> seSolapa(candidata.getHoraInicio(),
                                candidata.getHoraFin(), hora))
                        .filter(candidata -> candidata.getRecursos().stream()
                                .anyMatch(usado -> usado.getId().equals(recurso.getId())))
                        .findFirst()
                        .orElse(null);
                if (reserva == null) {
                    fila.add(CeldaCalendario.libre());
                } else {
                    fila.add(new CeldaCalendario(reserva.getActividad() + " — "
                            + reserva.getFuncionario().getNombre(), true));
                }
            }
            filas.add(fila);
        }
        return new MatrizCalendarizacion(fecha, categoria, recursos, horas, filas);
    }

    // ------------------------------------------------------------------
    // Estadísticas
    // ------------------------------------------------------------------

    @Override
    public List<ConteoCategoria> categoriasReservadas(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return reservaDAO.contarReservasPorCategoria(desde, hasta);
    }

    @Override
    public List<ConteoSemana> actividadesPorSemana(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return reservaDAO.contarActividadesPorSemana(desde, hasta);
    }

    // ------------------------------------------------------------------
    // Extracción con IA
    // ------------------------------------------------------------------

    @Override
    public DatosReservaSugerida extraerDatosReserva(String frase) {
        String texto = Validaciones.texto(frase, "La frase", MAX_FRASE_IA);
        List<CategoriaRecurso> categorias = categoriaDAO.listarTodas();
        PropuestaIA propuesta = clienteIA.extraer(texto, categorias);
        MapeoCategorias.ResultadoMapeo mapeo =
                MapeoCategorias.mapear(propuesta.categorias(), categorias);
        return new DatosReservaSugerida(propuesta.actividad(), propuesta.fecha(),
                propuesta.horaInicio(), propuesta.horaFin(), mapeo.encontradas(),
                mapeo.noEncontradas());
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    private Funcionario buscarFuncionario(String id) {
        String identificador = Validaciones.texto(id, "El funcionario", MAX_ID);
        Usuario usuario = usuarioDAO.buscarPorId(identificador)
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe el funcionario con identificador " + identificador + "."));
        if (!(usuario instanceof Funcionario funcionario)) {
            throw new NoEncontradoException(
                    "No existe el funcionario con identificador " + identificador + ".");
        }
        return funcionario;
    }

    private CategoriaRecurso buscarCategoria(String id) {
        String identificador = Validaciones.texto(id, "La categoría", MAX_ID);
        return categoriaDAO.buscarPorId(identificador)
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe la categoría con identificador " + identificador + "."));
    }

    private void validarHorarioReserva(LocalDate fecha, LocalTime horaInicio,
                                       LocalTime horaFin) {
        if (fecha == null) {
            throw new ValidacionException("La fecha no puede ser nula.");
        }
        if (fecha.isBefore(LocalDate.now(reloj))) {
            throw new ValidacionException("La fecha no puede ser anterior a hoy.");
        }
        if (horaInicio == null || horaFin == null) {
            throw new ValidacionException("Debe indicar la hora de inicio y de fin.");
        }
        if (!horaFin.isAfter(horaInicio)) {
            throw new ValidacionException(
                    "La hora de fin debe ser posterior a la hora de inicio.");
        }
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ValidacionException("Debe indicar la fecha desde y la fecha hasta.");
        }
        if (desde.isAfter(hasta)) {
            throw new ValidacionException(
                    "La fecha desde no puede ser posterior a la fecha hasta.");
        }
    }

    private static boolean coincide(String clave, String hash) {
        try {
            return BCrypt.checkpw(clave, hash);
        } catch (IllegalArgumentException ex) {
            // Hash malformado o de otro algoritmo: no se autentica.
            return false;
        }
    }

    private static List<LocalTime> horasDelDia() {
        List<LocalTime> horas = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            horas.add(LocalTime.of(i, 0));
        }
        return List.copyOf(horas);
    }

    /**
     * Una reserva ocupa el bloque de una hora si su intervalo se cruza con él.
     * Así una reserva de 09:30 a 10:30 aparece en las filas de 09:00 y 10:00.
     */
    private static boolean seSolapa(LocalTime inicio, LocalTime fin, LocalTime hora) {
        int inicioBloque = hora.toSecondOfDay() / 60;
        int finBloque = inicioBloque + 60;
        return fin.toSecondOfDay() / 60 > inicioBloque
                && inicio.toSecondOfDay() / 60 < finBloque;
    }
}
