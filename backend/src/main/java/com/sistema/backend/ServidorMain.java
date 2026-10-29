package com.sistema.backend;

/**
 * Punto de entrada del proceso Backend (servidor sin interfaz de usuario).
 *
 * <p>En la Fase 6 este main se reemplaza por el servidor de sockets que escucha
 * en el puerto configurado en {@code backend.properties}.</p>
 */
public final class ServidorMain {

    private ServidorMain() {
    }

    public static void main(String[] args) {
        System.out.println("[backend] Sistema de Reservas de Recursos - Proyecto 2");
        System.out.println("[backend] Java " + System.getProperty("java.version")
                + " | servidor de sockets pendiente (Fase 6).");
    }
}
