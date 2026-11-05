package com.sistema.backend;

import com.sistema.backend.config.ConfiguracionBackend;
import com.sistema.backend.data.FuenteDatos;
import com.sistema.backend.logic.ServiceImpl;
import com.sistema.backend.server.Servidor;
import java.net.BindException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class ServidorMain {

    private static final Logger LOG = LoggerFactory.getLogger(ServidorMain.class);
    private static final int INTENTOS_BD = 10;
    private static final long ESPERA_BD_MS = 1_000;

    private ServidorMain() {
    }

    public static void main(String[] args) {
        try {
            ejecutar();
        } catch (RuntimeException e) {
            LOG.error("El backend no pudo iniciarse: {}", mensajeDeInicio(e));
            LOG.debug("Detalle del fallo de inicio", e);
            System.exit(1);
        }
    }

    private static void ejecutar() {
        ConfiguracionBackend configuracion = ConfiguracionBackend.cargar();
        FuenteDatos fuente = abrirFuente(configuracion);
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

    private static FuenteDatos abrirFuente(ConfiguracionBackend configuracion) {
        RuntimeException ultimo = null;
        for (int intento = 1; intento <= INTENTOS_BD; intento++) {
            try {
                return new FuenteDatos(configuracion);
            } catch (RuntimeException e) {
                ultimo = e;
                if (intento < INTENTOS_BD) {
                    LOG.warn("MySQL no responde; reintentando en {} s ({}/{}).",
                            ESPERA_BD_MS / 1_000, intento, INTENTOS_BD);
                    try {
                        Thread.sleep(ESPERA_BD_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        if (ultimo != null) {
            throw ultimo;
        }
        throw new IllegalStateException("No se pudo conectar con MySQL.");
    }

    static String mensajeDeInicio(Throwable error) {
        Throwable causa = error;
        while (causa != null) {
            if (causa instanceof SQLException || causa instanceof ConnectException) {
                return "No se pudo conectar con MySQL. Inicie el servidor de base de datos "
                        + "y verifique bd.url, bd.usuario y bd.clave en backend.properties.";
            }
            if (causa instanceof BindException) {
                return "El puerto del backend ya está en uso. Cierre el backend anterior "
                        + "o cambie servidor.puerto en backend.properties.";
            }
            causa = causa.getCause();
        }
        String mensaje = error.getMessage();
        return mensaje == null || mensaje.isBlank()
                ? "Error desconocido al iniciar el backend." : mensaje;
    }
}
