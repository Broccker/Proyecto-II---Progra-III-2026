package com.sistema.frontend.controller;

import com.sistema.common.protocolo.Notificacion;
import com.sistema.frontend.proxy.EscuchaNotificaciones;
import com.sistema.frontend.proxy.Reconectable;
import com.sistema.frontend.view.Dialogos;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


public final class ControladorConexion implements EscuchaNotificaciones {

    private static final int MAX_INTENTOS = 3;

    private final Reconectable reconectable;
    private final EjecutorTareas ejecutor;
    private final HiloInterfaz hilo;
    private final Runnable alCerrar;
    private final AtomicBoolean atendiendo = new AtomicBoolean(false);

    private Runnable alRestablecer = () -> { };

    public ControladorConexion(Reconectable reconectable, EjecutorTareas ejecutor,
                               Runnable alCerrar) {
        this(reconectable, ejecutor, HiloInterfaz.swing(), alCerrar);
    }

    ControladorConexion(Reconectable reconectable, EjecutorTareas ejecutor,
                        HiloInterfaz hilo, Runnable alCerrar) {
        this.reconectable = Objects.requireNonNull(reconectable,
                "La conexión reconectable es obligatoria.");
        this.ejecutor = Objects.requireNonNull(ejecutor, "El ejecutor es obligatorio.");
        this.hilo = Objects.requireNonNull(hilo, "El hilo de interfaz es obligatorio.");
        this.alCerrar = Objects.requireNonNull(alCerrar, "La acción de cierre es obligatoria.");
    }

    @Override
    public void alRecibir(Notificacion notificacion) {

    }

    @Override
    public void alPerderConexion(Throwable causa) {
        if (!atendiendo.compareAndSet(false, true)) {
            return;
        }
        hilo.ejecutar(() -> preguntar(causa, 1));
    }


    public void alRestablecer(Runnable accion) {
        alRestablecer = accion == null ? () -> { } : accion;
    }

    private void preguntar(Throwable causa, int intento) {
        boolean reintentar = Dialogos.confirmar(null, "Conexión perdida",
                "Se perdió la conexión con el servidor.\n"
                        + Errores.mensaje(causa)
                        + "\n¿Desea volver a conectarse?");
        if (!reintentar) {
            cerrar();
            return;
        }

        ejecutor.ejecutar(reconectable::reconectar,
                () -> {
                    atendiendo.set(false);
                    alRestablecer.run();
                    Dialogos.informar(null, "Conexión restablecida",
                            "La conexión con el servidor se restableció.");
                },
                error -> {
                    if (intento >= MAX_INTENTOS) {
                        Dialogos.error(null, "Sin conexión",
                                "No fue posible reconectar con el servidor.\n"
                                        + Errores.mensaje(error)
                                        + "\nLa aplicación se cerrará.");
                        cerrar();
                    } else {
                        preguntar(error, intento + 1);
                    }
                });
    }

    private void cerrar() {
        atendiendo.set(false);
        alCerrar.run();
    }
}
