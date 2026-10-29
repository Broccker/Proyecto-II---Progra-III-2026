package com.sistema.backend.server;

import com.sistema.common.modelo.Reserva;
import com.sistema.common.protocolo.CodigoError;
import com.sistema.common.protocolo.Notificacion;
import com.sistema.common.protocolo.Operacion;
import com.sistema.common.protocolo.Protocolo;
import com.sistema.common.protocolo.Respuesta;
import com.sistema.common.protocolo.Solicitud;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Atiende a un cliente durante toda su conexión.
 *
 * <p>Se ejecuta en su propio hilo (virtual) y hace un bucle:
 * leer {@link Solicitud}, ejecutarla con el {@link Despachador}, escribir la
 * {@link Respuesta} con el mismo identificador de correlación y, si se creó
 * una reserva, difundir la {@link Notificacion} a los demás clientes. Al
 * desconectarse el cliente, o ante un error de escritura, se elimina del
 * registro y se cierra el socket.</p>
 */
public final class ManejadorCliente implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorCliente.class);

    /** Tiempo máximo para completar el saludo inicial del protocolo. */
    private static final int TIEMPO_SALUDO_MS = 10_000;

    private final Socket socket;
    private final String id;
    private final Despachador despachador;
    private final RegistroClientes registro;

    public ManejadorCliente(Socket socket, String id, Despachador despachador,
                            RegistroClientes registro) {
        this.socket = Objects.requireNonNull(socket, "El socket es obligatorio.");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador del cliente no puede estar vacío.");
        }
        this.id = id;
        this.despachador = Objects.requireNonNull(despachador, "El despachador es obligatorio.");
        this.registro = Objects.requireNonNull(registro, "El registro es obligatorio.");
    }

    @Override
    public void run() {
        ConexionCliente conexion = null;
        try {
            socket.setSoTimeout(TIEMPO_SALUDO_MS);
            conexion = new ConexionCliente(socket, id);
            socket.setSoTimeout(0);
            registro.registrar(conexion);
            LOG.info("Cliente {} conectado.", id);
            atender(conexion);
        } catch (EOFException e) {
            LOG.debug("El cliente {} cerró la conexión.", id);
        } catch (SocketTimeoutException e) {
            LOG.info("El cliente {} no completó el saludo del protocolo a tiempo.", id);
        } catch (SocketException e) {
            if (conexion != null && conexion.estaCerrado()) {
                LOG.debug("La conexión del cliente {} fue cerrada por el servidor.", id);
            } else {
                LOG.info("Conexión interrumpida con el cliente {}.", id);
            }
        } catch (IOException e) {
            LOG.warn("Error de entrada/salida con el cliente {}.", id, e);
        } catch (ClassNotFoundException e) {
            LOG.warn("El cliente {} envió un objeto no permitido por el protocolo.", id);
        } finally {
            if (conexion != null) {
                registro.eliminar(conexion);
                conexion.cerrar();
            } else {
                cerrarSilenciosamente(socket);
            }
            LOG.info("Cliente {} desconectado.", id);
        }
    }

    private void atender(ConexionCliente conexion) throws IOException, ClassNotFoundException {
        while (!conexion.estaCerrado()) {
            Object mensaje = conexion.leer();
            if (mensaje instanceof Solicitud solicitud) {
                atenderSolicitud(solicitud, conexion);
            } else {
                LOG.warn("El cliente {} envió un mensaje no reconocido ({}).", id,
                        mensaje == null ? "nulo" : mensaje.getClass().getSimpleName());
                conexion.enviar(Respuesta.error(Protocolo.nuevoId(), CodigoError.VALIDACION,
                        "Mensaje no reconocido por el servidor."));
            }
        }
    }

    private void atenderSolicitud(Solicitud solicitud, ConexionCliente conexion)
            throws IOException {
        Object resultado;
        try {
            resultado = despachador.ejecutar(solicitud);
        } catch (RuntimeException e) {
            CodigoError codigo = CodigoError.de(e);
            LOG.warn("Falló la operación {} del cliente {}: {} ({}).",
                    solicitud.operacion(), id, codigo, e.getClass().getSimpleName());
            LOG.debug("Detalle del error de la operación {} del cliente {}.",
                    solicitud.operacion(), id, e);
            conexion.enviar(Respuesta.error(solicitud.id(), e));
            return;
        }
        conexion.enviar(Respuesta.ok(solicitud.id(), resultado));
        if (solicitud.operacion() == Operacion.CREAR_RESERVA
                && resultado instanceof Reserva reserva) {
            registro.difundir(Notificacion.nuevaReserva(reserva), conexion);
        }
    }

    private static void cerrarSilenciosamente(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOG.debug("No se pudo cerrar el socket del cliente.", e);
        }
    }
}
