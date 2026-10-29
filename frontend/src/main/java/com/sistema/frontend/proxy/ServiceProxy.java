package com.sistema.frontend.proxy;

import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import com.sistema.common.dto.DatosReservaSugerida;
import com.sistema.common.dto.MatrizActividades;
import com.sistema.common.dto.MatrizCalendarizacion;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import com.sistema.common.modelo.Usuario;
import com.sistema.common.protocolo.Notificacion;
import com.sistema.common.protocolo.Operacion;
import com.sistema.common.protocolo.Respuesta;
import com.sistema.common.protocolo.Solicitud;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.config.ConfiguracionFrontend;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación de {@link Service} que habla con el backend por sockets.
 *
 * <p>Es el único punto del frontend que conoce el protocolo. Los
 * controladores reciben un {@code Service} por inyección y no distinguen este
 * proxy de una implementación local.</p>
 *
 * <p>Por cada llamada se crea una {@link Solicitud} con un identificador de
 * correlación nuevo y se registra un {@link CompletableFuture} a la espera de
 * la {@link Respuesta}. Un hilo lector dedicado recibe los mensajes y los
 * separa:</p>
 * <ul>
 *   <li>la respuesta se entrega al futuro que coincide con su id;</li>
 *   <li>la notificación se reenvía a las {@link EscuchaNotificaciones}
 *       registradas (Fase 9).</li>
 * </ul>
 *
 * <p>Las llamadas son bloqueantes desde el punto de vista de quien las hace;
 * en Swing deben ejecutarse fuera del hilo de eventos (por ejemplo, con
 * {@code SwingWorker}).</p>
 */
public final class ServiceProxy implements Service, FuenteNotificaciones, Reconectable,
        AutoCloseable {

    private static final Logger LOG = Logger.getLogger(ServiceProxy.class.getName());

    private final ConfiguracionFrontend configuracion;
    private final Map<String, CompletableFuture<Respuesta>> pendientes =
            new ConcurrentHashMap<>();
    private final List<EscuchaNotificaciones> escuchas = new CopyOnWriteArrayList<>();
    private final AtomicBoolean cerrado = new AtomicBoolean(false);
    private final AtomicBoolean conectado = new AtomicBoolean(false);
    private final AtomicLong generacion = new AtomicLong();
    private final Object bloqueoConexion = new Object();

    private volatile ConexionServidor conexion;
    private volatile LectorMensajes lector;
    private volatile Thread hiloLector;
    private volatile Throwable causaDeCorte;

    /**
     * Abre la conexión con el backend y arranca el hilo lector.
     *
     * @throws ExcepcionRed si el servidor no está disponible o el protocolo
     *         no arranca
     */
    public ServiceProxy(ConfiguracionFrontend configuracion) {
        this.configuracion = Objects.requireNonNull(configuracion,
                "La configuración del frontend es obligatoria.");
        abrirConexion();
    }

    /**
     * Configuración con la que se abrió esta conexión.
     */
    public ConfiguracionFrontend configuracion() {
        return configuracion;
    }

    /**
     * Registra un receptor de notificaciones. Es seguro llamarlo desde el
     * hilo de la interfaz mientras otro hilo difunde.
     */
    @Override
    public void agregarEscucha(EscuchaNotificaciones escucha) {
        Objects.requireNonNull(escucha, "La escucha es obligatoria.");
        escuchas.add(escucha);
    }

    /**
     * Quita un receptor previamente registrado.
     */
    @Override
    public void quitarEscucha(EscuchaNotificaciones escucha) {
        escuchas.remove(escucha);
    }

    /**
     * Indica si la conexión sigue abierta y utilizable.
     */
    @Override
    public boolean estaConectado() {
        return !cerrado.get() && conectado.get();
    }

    /**
     * Cierra la conexión y falla las peticiones pendientes. Es idempotente.
     */
    @Override
    public void close() {
        synchronized (bloqueoConexion) {
            if (!cerrado.compareAndSet(false, true)) {
                return;
            }
            cerrarConexionActual(ExcepcionRed.cerrado());
        }
    }

    /**
     * Reemplaza la conexión actual por una nueva con el mismo host y puerto.
     *
     * <p>Se usa cuando el backend se recuperó después de una caída. Mientras
     * dura la reconexión el proxy rechaza peticiones; las escuchas registradas
     * se conservan. Si el servidor sigue apagado, el proxy queda desconectado
     * y se puede volver a intentar.</p>
     *
     * @throws ExcepcionRed si no se puede abrir la nueva conexión
     */
    @Override
    public void reconectar() {
        synchronized (bloqueoConexion) {
            if (cerrado.get()) {
                throw ExcepcionRed.cerrado();
            }
            cerrarConexionActual(ExcepcionRed.conexionPerdida(causaDeCorte));
            try {
                abrirConexion();
            } catch (ExcepcionRed e) {
                causaDeCorte = e;
                conectado.set(false);
                throw e;
            }
        }
    }

    /**
     * Abre el socket, arranca el lector y deja el proxy disponible.
     */
    private void abrirConexion() {
        ConexionServidor nueva;
        try {
            nueva = ConexionServidor.abrir(configuracion.host(), configuracion.puerto());
        } catch (IOException e) {
            throw ExcepcionRed.sinConexion(configuracion.host(), configuracion.puerto(), e);
        }
        long nuevaGeneracion = generacion.incrementAndGet();
        this.conexion = nueva;
        this.lector = new LectorMensajes(nueva.entrada(), new Receptor(nuevaGeneracion));
        this.causaDeCorte = null;
        this.conectado.set(true);
        this.hiloLector = Thread.ofPlatform()
                .name("proxy-lector-" + nuevaGeneracion)
                .daemon(true)
                .start(lector);
    }

    /**
     * Invalida la generación actual, falla lo pendiente y cierra el socket.
     * El incremento de generación evita que un lector viejo marque como caída
     * la conexión nueva después de una reconexión.
     */
    private void cerrarConexionActual(Throwable causaPendientes) {
        generacion.incrementAndGet();
        conectado.set(false);
        fallarPendientes(causaPendientes);

        LectorMensajes lectorActual = lector;
        ConexionServidor conexionActual = conexion;
        Thread hiloActual = hiloLector;
        if (lectorActual != null) {
            lectorActual.detener();
        }
        if (conexionActual != null) {
            conexionActual.close();
        }
        if (hiloActual != null && hiloActual != Thread.currentThread()) {
            hiloActual.interrupt();
            try {
                hiloActual.join(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lector = null;
        conexion = null;
        hiloLector = null;
    }

    // ------------------------------------------------------------------
    // Sesión
    // ------------------------------------------------------------------

    @Override
    public Usuario iniciarSesion(String id, String clave) {
        return objeto(invocar(Operacion.INICIAR_SESION, id, clave), Usuario.class);
    }

    @Override
    public void cambiarClave(String id, String claveActual, String nuevaClave,
                             String confirmacion) {
        invocar(Operacion.CAMBIAR_CLAVE, id, claveActual, nuevaClave, confirmacion);
    }

    // ------------------------------------------------------------------
    // Funcionarios
    // ------------------------------------------------------------------

    @Override
    public void altaFuncionario(String id, String nombre, String telefono) {
        invocar(Operacion.ALTA_FUNCIONARIO, id, nombre, telefono);
    }

    @Override
    public void modificarFuncionario(String id, String nombre, String telefono) {
        invocar(Operacion.MODIFICAR_FUNCIONARIO, id, nombre, telefono);
    }

    @Override
    public void eliminarFuncionario(String id) {
        invocar(Operacion.ELIMINAR_FUNCIONARIO, id);
    }

    @Override
    public List<Funcionario> listarFuncionarios() {
        return lista(invocar(Operacion.LISTAR_FUNCIONARIOS), Funcionario.class);
    }

    @Override
    public List<Funcionario> buscarFuncionarios(String criterio) {
        return lista(invocar(Operacion.BUSCAR_FUNCIONARIOS, criterio), Funcionario.class);
    }

    // ------------------------------------------------------------------
    // Categorías de recursos
    // ------------------------------------------------------------------

    @Override
    public CategoriaRecurso altaCategoria(String descripcion) {
        return objeto(invocar(Operacion.ALTA_CATEGORIA, descripcion), CategoriaRecurso.class);
    }

    @Override
    public void modificarCategoria(String id, String nuevoId, String descripcion) {
        invocar(Operacion.MODIFICAR_CATEGORIA, id, nuevoId, descripcion);
    }

    @Override
    public void eliminarCategoria(String id) {
        invocar(Operacion.ELIMINAR_CATEGORIA, id);
    }

    @Override
    public List<CategoriaRecurso> listarCategorias() {
        return lista(invocar(Operacion.LISTAR_CATEGORIAS), CategoriaRecurso.class);
    }

    @Override
    public List<CategoriaRecurso> buscarCategorias(String criterio) {
        return lista(invocar(Operacion.BUSCAR_CATEGORIAS, criterio), CategoriaRecurso.class);
    }

    @Override
    public boolean existeDescripcionCategoria(String descripcion) {
        Object resultado = invocar(Operacion.EXISTE_DESCRIPCION_CATEGORIA, descripcion);
        if (resultado instanceof Boolean valor) {
            return valor;
        }
        throw ExcepcionRed.respuestaInvalida(
                "Se esperaba un booleano y llegó " + tipoDe(resultado) + ".");
    }

    // ------------------------------------------------------------------
    // Recursos
    // ------------------------------------------------------------------

    @Override
    public void altaRecurso(String id, String categoriaId, String descripcion) {
        invocar(Operacion.ALTA_RECURSO, id, categoriaId, descripcion);
    }

    @Override
    public void modificarRecurso(String id, String nuevoId, String categoriaId,
                                 String descripcion) {
        invocar(Operacion.MODIFICAR_RECURSO, id, nuevoId, categoriaId, descripcion);
    }

    @Override
    public void eliminarRecurso(String id) {
        invocar(Operacion.ELIMINAR_RECURSO, id);
    }

    @Override
    public List<Recurso> listarRecursos() {
        return lista(invocar(Operacion.LISTAR_RECURSOS), Recurso.class);
    }

    @Override
    public List<Recurso> buscarRecursos(String criterio) {
        return lista(invocar(Operacion.BUSCAR_RECURSOS, criterio), Recurso.class);
    }

    @Override
    public List<Recurso> filtrarRecursos(String categoriaId, String criterio) {
        return lista(invocar(Operacion.FILTRAR_RECURSOS, categoriaId, criterio),
                Recurso.class);
    }

    // ------------------------------------------------------------------
    // Reservas
    // ------------------------------------------------------------------

    @Override
    public Reserva crearReserva(String funcionarioId, String actividad, LocalDate fecha,
                                LocalTime horaInicio, LocalTime horaFin,
                                List<String> idsCategorias) {
        return objeto(invocar(Operacion.CREAR_RESERVA, funcionarioId, actividad, fecha,
                horaInicio, horaFin, idsCategorias), Reserva.class);
    }

    @Override
    public void cancelarReserva(String idReserva, String funcionarioId) {
        invocar(Operacion.CANCELAR_RESERVA, idReserva, funcionarioId);
    }

    @Override
    public List<Reserva> listarReservasDe(String funcionarioId) {
        return lista(invocar(Operacion.LISTAR_RESERVAS_DE, funcionarioId), Reserva.class);
    }

    @Override
    public List<Reserva> listarTodasLasReservas() {
        return lista(invocar(Operacion.LISTAR_TODAS_LAS_RESERVAS), Reserva.class);
    }

    @Override
    public List<Reserva> listarReservasActivasEntre(LocalDate desde, LocalDate hasta) {
        return lista(invocar(Operacion.LISTAR_RESERVAS_ACTIVAS_ENTRE, desde, hasta),
                Reserva.class);
    }

    // ------------------------------------------------------------------
    // Actividades y calendarización
    // ------------------------------------------------------------------

    @Override
    public MatrizActividades construirMatrizActividades(LocalDate fecha) {
        return objeto(invocar(Operacion.CONSTRUIR_MATRIZ_ACTIVIDADES, fecha),
                MatrizActividades.class);
    }

    @Override
    public MatrizCalendarizacion construirMatrizCalendarizacion(LocalDate fecha,
                                                                String categoriaId) {
        return objeto(invocar(Operacion.CONSTRUIR_MATRIZ_CALENDARIZACION, fecha, categoriaId),
                MatrizCalendarizacion.class);
    }

    // ------------------------------------------------------------------
    // Estadísticas
    // ------------------------------------------------------------------

    @Override
    public List<ConteoCategoria> categoriasReservadas(LocalDate desde, LocalDate hasta) {
        return lista(invocar(Operacion.CATEGORIAS_RESERVADAS, desde, hasta),
                ConteoCategoria.class);
    }

    @Override
    public List<ConteoSemana> actividadesPorSemana(LocalDate desde, LocalDate hasta) {
        return lista(invocar(Operacion.ACTIVIDADES_POR_SEMANA, desde, hasta),
                ConteoSemana.class);
    }

    // ------------------------------------------------------------------
    // Extracción con IA
    // ------------------------------------------------------------------

    @Override
    public DatosReservaSugerida extraerDatosReserva(String frase) {
        return objeto(invocar(Operacion.EXTRAER_DATOS_RESERVA, frase),
                DatosReservaSugerida.class);
    }

    // ------------------------------------------------------------------
    // Transporte
    // ------------------------------------------------------------------

    private Object invocar(Operacion operacion, Object... parametros) {
        Objects.requireNonNull(operacion, "La operación es obligatoria.");
        verificarDisponible();

        Solicitud solicitud = Solicitud.de(operacion, parametros);
        CompletableFuture<Respuesta> espera = new CompletableFuture<>();
        pendientes.put(solicitud.id(), espera);
        long generacionActual = generacion.get();
        ConexionServidor actual = conexion;
        try {
            if (actual == null) {
                throw new IOException("La conexión con el servidor no está disponible.");
            }
            actual.enviar(solicitud);
        } catch (IOException e) {
            pendientes.remove(solicitud.id());
            perderConexion(e, generacionActual);
            throw ExcepcionRed.conexionPerdida(e);
        }

        try {
            Respuesta respuesta = espera.get(configuracion.tiempoEsperaMs(),
                    TimeUnit.MILLISECONDS);
            if (!respuesta.exito()) {
                throw TraductorErrores.aExcepcion(respuesta.error());
            }
            return respuesta.resultado();
        } catch (TimeoutException e) {
            pendientes.remove(solicitud.id());
            throw ExcepcionRed.tiempoAgotado(operacion, configuracion.tiempoEsperaMs());
        } catch (InterruptedException e) {
            pendientes.remove(solicitud.id());
            Thread.currentThread().interrupt();
            throw ExcepcionRed.interrumpida(e);
        } catch (ExecutionException e) {
            pendientes.remove(solicitud.id());
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException error) {
                throw error;
            }
            throw ExcepcionRed.conexionPerdida(causa);
        }
    }

    private void verificarDisponible() {
        if (cerrado.get()) {
            throw ExcepcionRed.cerrado();
        }
        if (!conectado.get()) {
            if (causaDeCorte instanceof ExcepcionRed error) {
                throw error;
            }
            throw ExcepcionRed.conexionPerdida(causaDeCorte);
        }
    }

    /**
     * Falla las peticiones pendientes y avisa a las escuchas, una sola vez.
     *
     * <p>Solo se atiende el corte de la generación vigente: un lector viejo
     * que termina después de una reconexión no debe marcar como caída la
     * conexión nueva.</p>
     */
    private void perderConexion(Throwable causa, long generacionAfectada) {
        if (generacion.get() != generacionAfectada) {
            return;
        }
        if (cerrado.get() || !conectado.compareAndSet(true, false)) {
            return;
        }
        causaDeCorte = causa;
        fallarPendientes(ExcepcionRed.conexionPerdida(causa));
        for (EscuchaNotificaciones escucha : escuchas) {
            try {
                escucha.alPerderConexion(causa);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Una escucha falló al reportar la pérdida de conexión.", e);
            }
        }
    }

    private void fallarPendientes(Throwable causa) {
        for (String id : List.copyOf(pendientes.keySet())) {
            CompletableFuture<Respuesta> espera = pendientes.remove(id);
            if (espera != null) {
                espera.completeExceptionally(causa);
            }
        }
    }

    private void entregarNotificacion(Notificacion notificacion) {
        for (EscuchaNotificaciones escucha : escuchas) {
            try {
                escucha.alRecibir(notificacion);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING,
                        "Una escucha falló al procesar la notificación " + notificacion.tipo() + ".", e);
            }
        }
    }

    private static <T> T objeto(Object resultado, Class<T> tipo) {
        if (!tipo.isInstance(resultado)) {
            throw ExcepcionRed.respuestaInvalida(
                    "Se esperaba " + tipo.getSimpleName() + " y llegó "
                            + tipoDe(resultado) + ".");
        }
        return tipo.cast(resultado);
    }

    private static <T> List<T> lista(Object resultado, Class<T> tipo) {
        if (!(resultado instanceof List<?> elementos)) {
            throw ExcepcionRed.respuestaInvalida(
                    "Se esperaba una lista y llegó " + tipoDe(resultado) + ".");
        }
        List<T> copia = new ArrayList<>(elementos.size());
        for (Object elemento : elementos) {
            if (!tipo.isInstance(elemento)) {
                throw ExcepcionRed.respuestaInvalida(
                        "La lista debía contener " + tipo.getSimpleName() + " y llegó "
                                + tipoDe(elemento) + ".");
            }
            copia.add(tipo.cast(elemento));
        }
        return List.copyOf(copia);
    }

    private static String tipoDe(Object valor) {
        return valor == null ? "null" : valor.getClass().getSimpleName();
    }

    /**
     * Receptor que el hilo lector invoca para entregar cada mensaje.
     */
    private final class Receptor implements LectorMensajes.Receptor {

        private final long generacionDeLaConexion;

        private Receptor(long generacionDeLaConexion) {
            this.generacionDeLaConexion = generacionDeLaConexion;
        }

        @Override
        public void alRecibirRespuesta(Respuesta respuesta) {
            CompletableFuture<Respuesta> espera = pendientes.remove(respuesta.id());
            if (espera == null) {
                // Puede ser una respuesta que llegó después del timeout.
                LOG.fine(() -> "Respuesta sin petición pendiente: " + respuesta.id());
                return;
            }
            espera.complete(respuesta);
        }

        @Override
        public void alRecibirNotificacion(Notificacion notificacion) {
            entregarNotificacion(notificacion);
        }

        @Override
        public void alPerderConexion(Throwable causa) {
            perderConexion(causa, generacionDeLaConexion);
        }
    }
}
