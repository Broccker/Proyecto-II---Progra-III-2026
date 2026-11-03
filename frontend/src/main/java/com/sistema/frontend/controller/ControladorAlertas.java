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


public final class ControladorAlertas implements EscuchaNotificaciones {

    private final ModeloAlertas modelo;
    private final PanelAlertas vista;
    private final HiloInterfaz hilo;
    private final Queue<CargaNuevaReserva> pendientes = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean drenajeProgramado = new AtomicBoolean();
    private final AtomicBoolean desconexionAvisada = new AtomicBoolean();


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


    @Override
    public void alRecibir(Notificacion notificacion) {
        if (notificacion == null || notificacion.tipo() != TipoNotificacion.NUEVA_RESERVA) {
            return;
        }
        pendientes.add(notificacion.comoNuevaReserva());
        programarDrenaje();
    }


    @Override
    public void alPerderConexion(Throwable causa) {
        if (!desconexionAvisada.compareAndSet(false, true)) {
            return;
        }
        String mensaje = Errores.mensaje(causa);
        hilo.ejecutar(() -> vista.mostrarDesconexion(mensaje));
    }


    public void conexionRestablecida() {
        desconexionAvisada.set(false);
        hilo.ejecutar(vista::mostrarConexion);
    }


    public void ver(int fila) {
        Alerta alerta = alertaEn(fila);
        if (alerta != null) {
            vista.mostrarDetalle(alerta);
        }
    }


    public void descartar(int fila) {
        Alerta alerta = alertaEn(fila);
        if (alerta == null || !vista.confirmarDescarte(alerta)) {
            return;
        }
        modelo.eliminar(alerta);
        vista.actualizar();
    }


    public void vaciar() {
        pendientes.clear();
        modelo.vaciar();
        vista.actualizar();
    }


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
