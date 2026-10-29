package com.sistema.frontend.controller;

import com.sistema.common.protocolo.CargaNuevaReserva;
import com.sistema.common.protocolo.Notificacion;
import com.sistema.common.protocolo.TipoNotificacion;
import com.sistema.frontend.model.Alerta;
import com.sistema.frontend.model.ModeloAlertas;
import com.sistema.frontend.proxy.EscuchaNotificaciones;
import com.sistema.frontend.view.PanelAlertas;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controlador del área de alertas.
 *
 * <p>Se registra como {@link EscuchaNotificaciones} del proxy. Cuando llega una
 * reserva de otro usuario la encola y programa una única actualización en el
 * hilo de la interfaz: si llegan muchas notificaciones seguidas se drenan en un
 * solo refresco, sin bloquear el hilo lector ni el de eventos.</p>
 *
 * <p>Las alertas son locales y efímeras: "Ver" abre el detalle y "Descartar"
 * solo las quita del modelo. Nunca se envía una petición al backend.</p>
 */
public final class ControladorAlertas implements EscuchaNotificaciones {

    private final ModeloAlertas modelo;
    private final PanelAlertas vista;
    private final HiloInterfaz hilo;
    private final Queue<CargaNuevaReserva> pendientes = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean drenajeProgramado = new AtomicBoolean();
    private final AtomicBoolean desconexionAvisada = new AtomicBoolean();

    /** Versión de producción: las actualizaciones se hacen en el EDT. */
    public ControladorAlertas(ModeloAlertas modelo, PanelAlertas vista) {
        this(modelo, vista, HiloInterfaz.swing());
    }

    ControladorAlertas(ModeloAlertas modelo, PanelAlertas vista, HiloInterfaz hilo) {
        this.modelo = Objects.requireNonNull(modelo, "El modelo es obligatorio.");
        this.vista = Objects.requireNonNull(vista, "La vista es obligatoria.");
        this.hilo = Objects.requireNonNull(hilo, "El hilo de interfaz es obligatorio.");

        vista.alVer(this::ver);
        vista.alDescartar(this::descartar);
        vista.actualizar();
    }

    /**
     * Recibe la notificación en el hilo lector del proxy y la programa para el
     * hilo de la interfaz. No toca componentes Swing desde este hilo.
     */
    @Override
    public void alRecibir(Notificacion notificacion) {
        if (notificacion == null || notificacion.tipo() != TipoNotificacion.NUEVA_RESERVA) {
            return;
        }
        pendientes.add(notificacion.comoNuevaReserva());
        programarDrenaje();
    }

    /** Avisa al usuario, una sola vez, que el servidor dejó de responder. */
    @Override
    public void alPerderConexion(Throwable causa) {
        if (!desconexionAvisada.compareAndSet(false, true)) {
            return;
        }
        String mensaje = Errores.mensaje(causa);
        hilo.ejecutar(() -> vista.mostrarDesconexion(mensaje));
    }

    /** Abre el detalle de la alerta de esa fila. */
    public void ver(int fila) {
        Alerta alerta = alertaEn(fila);
        if (alerta != null) {
            vista.mostrarDetalle(alerta);
        }
    }

    /** Descarta la alerta de esa fila, solo de la lista local. */
    public void descartar(int fila) {
        Alerta alerta = alertaEn(fila);
        if (alerta == null || !vista.confirmarDescarte(alerta)) {
            return;
        }
        modelo.eliminar(alerta);
        vista.actualizar();
    }

    /** Descarta todas las alertas al cerrar la sesión. */
    public void vaciar() {
        pendientes.clear();
        modelo.vaciar();
        vista.actualizar();
    }

    /** Alerta visible en la fila indicada, o {@code null} si no existe. */
    Alerta alertaEn(int fila) {
        List<Alerta> alertas = modelo.getAlertas();
        if (fila < 0 || fila >= alertas.size()) {
            return null;
        }
        return alertas.get(fila);
    }

    private void programarDrenaje() {
        if (drenajeProgramado.compareAndSet(false, true)) {
            hilo.ejecutar(this::drenar);
        }
    }

    private void drenar() {
        drenajeProgramado.set(false);
        boolean huboCambios = false;
        CargaNuevaReserva carga;
        while ((carga = pendientes.poll()) != null) {
            if (modelo.agregar(Alerta.desde(carga))) {
                huboCambios = true;
            }
        }
        if (huboCambios) {
            vista.actualizar();
        }
    }
}
