package com.sistema.frontend.proxy;

import com.sistema.common.protocolo.Notificacion;
import com.sistema.common.protocolo.Protocolo;
import com.sistema.common.protocolo.Respuesta;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


final class LectorMensajes implements Runnable {


    interface Receptor {

        void alRecibirRespuesta(Respuesta respuesta);

        void alRecibirNotificacion(Notificacion notificacion);

        void alPerderConexion(Throwable causa);
    }

    private static final Logger LOG = Logger.getLogger(LectorMensajes.class.getName());

    private final ObjectInputStream entrada;
    private final Receptor receptor;
    private final AtomicBoolean activo = new AtomicBoolean(true);

    LectorMensajes(ObjectInputStream entrada, Receptor receptor) {
        this.entrada = Objects.requireNonNull(entrada, "El flujo de entrada es obligatorio.");
        this.receptor = Objects.requireNonNull(receptor, "El receptor es obligatorio.");
    }

    @Override
    public void run() {
        try {
            while (activo.get()) {
                Object mensaje = Protocolo.recibir(entrada);
                if (mensaje instanceof Respuesta respuesta) {
                    receptor.alRecibirRespuesta(respuesta);
                } else if (mensaje instanceof Notificacion notificacion) {
                    receptor.alRecibirNotificacion(notificacion);
                } else {
                    LOG.warning("El servidor envió un mensaje no reconocido: "
                            + (mensaje == null ? "nulo" : mensaje.getClass().getName()));
                }
            }
        } catch (EOFException e) {
            informarCorte(e);
        } catch (IOException e) {
            informarCorte(e);
        } catch (ClassNotFoundException | RuntimeException e) {
            informarCorte(e);
        }
    }


    void detener() {
        activo.set(false);
    }

    private void informarCorte(Throwable causa) {
        if (activo.getAndSet(false)) {
            LOG.log(Level.FINE, "Se cerró el flujo de entrada del backend.", causa);
            receptor.alPerderConexion(causa);
        }
    }
}
