package com.sistema.common.protocolo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


abstract class PruebaProtocoloBase {

    @SuppressWarnings("unchecked")
    protected static <T> T idaYVuelta(T mensaje) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream salida = Protocolo.crearSalida(bytes)) {
                Protocolo.enviar(salida, mensaje);
            }
            try (ObjectInputStream entrada = Protocolo.crearEntrada(
                    new ByteArrayInputStream(bytes.toByteArray()))) {
                return (T) Protocolo.recibir(entrada);
            }
        } catch (IOException | ClassNotFoundException ex) {
            throw new AssertionError("No se pudo serializar el mensaje de prueba.", ex);
        }
    }
}
