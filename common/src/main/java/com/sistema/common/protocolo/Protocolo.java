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


public final class Protocolo {


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


    public static ObjectOutputStream crearSalida(OutputStream salida) throws IOException {
        Objects.requireNonNull(salida, "El flujo de salida es obligatorio.");
        return new ObjectOutputStream(salida);
    }


    public static ObjectInputStream crearEntrada(InputStream entrada) throws IOException {
        return crearEntrada(entrada, MAX_BYTES);
    }


    static ObjectInputStream crearEntrada(InputStream entrada, long maxBytes) throws IOException {
        Objects.requireNonNull(entrada, "El flujo de entrada es obligatorio.");
        EntradaProtocolo objeto = new EntradaProtocolo(new FlujoLimitado(entrada, maxBytes));
        objeto.setObjectInputFilter(FILTRO);
        return objeto;
    }


    public static void enviar(ObjectOutputStream salida, Object mensaje) throws IOException {
        Objects.requireNonNull(salida, "El flujo de salida es obligatorio.");
        Objects.requireNonNull(mensaje, "El mensaje es obligatorio.");
        salida.writeObject(mensaje);
        salida.flush();
        salida.reset();
    }


    public static Object recibir(ObjectInputStream entrada)
            throws IOException, ClassNotFoundException {
        Objects.requireNonNull(entrada, "El flujo de entrada es obligatorio.");
        Object mensaje = entrada.readObject();
        if (entrada instanceof EntradaProtocolo protocolo) {
            protocolo.reiniciarLimite();
        }
        return mensaje;
    }


    public static String nuevoId() {
        return UUID.randomUUID().toString();
    }


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
