package com.sistema.backend;

import com.sistema.backend.config.ConfiguracionBackend;
import com.sistema.backend.data.FuenteDatos;
import com.sistema.backend.logic.ServiceImpl;
import com.sistema.backend.server.Servidor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada del proceso Backend (servidor sin interfaz de usuario).
 *
 * <p>Carga la configuración, crea el pool de conexiones y el servicio, abre el
 * puerto configurado y queda esperando peticiones. Un <em>shutdown hook</em>
 * cierra el servidor y el pool aunque el proceso reciba una señal de término,
 * para no dejar sockets ni conexiones a MySQL abiertas.</p>
 */
public final class ServidorMain {

    private static final Logger LOG = LoggerFactory.getLogger(ServidorMain.class);

    private ServidorMain() {
    }

    public static void main(String[] args) {
        try {
            ejecutar();
        } catch (RuntimeException e) {
            LOG.error("El backend no pudo iniciarse: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void ejecutar() {
        ConfiguracionBackend configuracion = ConfiguracionBackend.cargar();
        FuenteDatos fuente = new FuenteDatos(configuracion);
        Servidor servidor;
        try {
            servidor = new Servidor(configuracion.puertoServidor(),
                    new ServiceImpl(fuente, configuracion));
        } catch (RuntimeException e) {
            fuente.close();
            throw e;
        }

        AtomicBoolean cerrado = new AtomicBoolean(false);
        Runnable cierre = () -> {
            if (cerrado.compareAndSet(false, true)) {
                servidor.detener();
                fuente.close();
                LOG.info("Recursos del backend liberados.");
            }
        };
        Runtime.getRuntime().addShutdownHook(new Thread(cierre, "apagado-backend"));

        try {
            servidor.iniciar();
            LOG.info("Backend iniciado con Java {}; escuchando en el puerto {}.",
                    System.getProperty("java.version"), servidor.puerto());
            servidor.esperar();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            cierre.run();
        }
    }
}
