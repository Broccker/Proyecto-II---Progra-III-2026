package com.sistema.common.protocolo;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.Objects;
import java.util.UUID;

/**
 * Utilidades de transporte del protocolo.
 *
 * <p>Se usa <strong>serialización Java</strong> porque ambos extremos comparten
 * exactamente las mismas clases de {@code common}: conserva tipos como
 * {@code LocalDate}, enums y objetos del dominio sin dependencias externas ni
 * conversiones manuales. Cada mensaje es un objeto completo en el flujo; el
 * lector hace {@code readObject()} en un bucle.</p>
 *
 * <p>Como la deserialización de datos no confiables es peligrosa, el lector
 * aplica un filtro que solo acepta las clases del sistema y las de la
 * biblioteca estándar necesarias (texto, fechas y colecciones), además de
 * límites de profundidad, referencias y tamaño. El límite de 32 MB se aplica
 * a <strong>cada mensaje</strong>, no al total de la conexión, porque un
 * cliente y el servidor mantienen el mismo par de flujos durante toda la
 * sesión.</p>
 */
public final class Protocolo {

    /** Versión del protocolo, para detectar clientes incompatibles en el futuro. */
    public static final int VERSION = 1;

    private static final long MAX_BYTES = 32L * 1024 * 1024;
    private static final long MAX_ARRAY = 1_000_000;
    private static final long MAX_PROFUNDIDAD = 100;
    private static final long MAX_REFERENCIAS = 1_000_000;

    private static final ObjectInputFilter FILTRO = info -> {
        if (info.depth() > MAX_PROFUNDIDAD
                || info.references() > MAX_REFERENCIAS
                || info.arrayLength() > MAX_ARRAY) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> tipo = info.serialClass();
        if (tipo == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        while (tipo.isArray()) {
            tipo = tipo.getComponentType();
        }
        if (tipo.isPrimitive()) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        String nombre = tipo.getName();
        if (nombre.startsWith("com.sistema.common.")
                || nombre.startsWith("java.lang.")
                || nombre.startsWith("java.time.")
                || nombre.startsWith("java.util.")
                || nombre.startsWith("java.math.")) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        return ObjectInputFilter.Status.REJECTED;
    };

    private Protocolo() {
    }

    /**
     * Crea el flujo de salida de un socket. El encabezado del stream se
     * escribe al construir el objeto.
     */
    public static ObjectOutputStream crearSalida(OutputStream salida) throws IOException {
        Objects.requireNonNull(salida, "El flujo de salida es obligatorio.");
        return new ObjectOutputStream(salida);
    }

    /**
     * Crea el flujo de entrada con el filtro de clases permitidas y un límite
     * de 32 MB por mensaje.
     */
    public static ObjectInputStream crearEntrada(InputStream entrada) throws IOException {
        return crearEntrada(entrada, MAX_BYTES);
    }

    /**
     * Variante con límite explícito, usada por las pruebas del transporte.
     */
    static ObjectInputStream crearEntrada(InputStream entrada, long maxBytes) throws IOException {
        Objects.requireNonNull(entrada, "El flujo de entrada es obligatorio.");
        EntradaProtocolo objeto = new EntradaProtocolo(new FlujoLimitado(entrada, maxBytes));
        objeto.setObjectInputFilter(FILTRO);
        return objeto;
    }

    /**
     * Escribe un mensaje, lo envía y limpia la caché del stream.
     *
     * <p>{@code reset()} es importante para que un objeto modificado se envíe
     * completo la próxima vez y no como una referencia al ya enviado.</p>
     */
    public static void enviar(ObjectOutputStream salida, Object mensaje) throws IOException {
        Objects.requireNonNull(salida, "El flujo de salida es obligatorio.");
        Objects.requireNonNull(mensaje, "El mensaje es obligatorio.");
        salida.writeObject(mensaje);
        salida.flush();
        salida.reset();
    }

    /**
     * Lee el siguiente mensaje del flujo.
     *
     * <p>Al terminar un mensaje se reinicia el contador de bytes: el límite de
     * 32 MB protege cada mensaje, no la sesión completa.</p>
     */
    public static Object recibir(ObjectInputStream entrada)
            throws IOException, ClassNotFoundException {
        Objects.requireNonNull(entrada, "El flujo de entrada es obligatorio.");
        Object mensaje = entrada.readObject();
        if (entrada instanceof EntradaProtocolo protocolo) {
            protocolo.reiniciarLimite();
        }
        return mensaje;
    }

    /**
     * Genera un identificador de correlación único.
     */
    public static String nuevoId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Flujo de entrada que corta un mensaje que supere el límite indicado.
     */
    private static final class FlujoLimitado extends FilterInputStream {

        private final long limite;
        private long leidos;

        FlujoLimitado(InputStream entrada, long limite) {
            super(entrada);
            this.limite = limite;
        }

        @Override
        public int read() throws IOException {
            int dato = super.read();
            if (dato >= 0) {
                contar(1);
            }
            return dato;
        }

        @Override
        public int read(byte[] destino, int inicio, int longitud) throws IOException {
            int cantidad = super.read(destino, inicio, longitud);
            if (cantidad > 0) {
                contar(cantidad);
            }
            return cantidad;
        }

        @Override
        public long skip(long cantidad) throws IOException {
            long saltados = super.skip(cantidad);
            if (saltados > 0) {
                contar(saltados);
            }
            return saltados;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        void reiniciar() {
            leidos = 0;
        }

        private void contar(long cantidad) throws IOException {
            leidos += cantidad;
            if (leidos > limite) {
                throw new StreamCorruptedException(
                        "El mensaje supera el máximo de " + limite + " bytes.");
            }
        }
    }

    /**
     * Flujo de entrada que conserva una referencia al contador para poder
     * reiniciarlo después de cada mensaje.
     */
    private static final class EntradaProtocolo extends ObjectInputStream {

        private final FlujoLimitado flujo;

        EntradaProtocolo(FlujoLimitado flujo) throws IOException {
            super(flujo);
            this.flujo = flujo;
        }

        void reiniciarLimite() {
            flujo.reiniciar();
        }
    }
}
