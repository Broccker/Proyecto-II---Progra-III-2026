package com.sistema.common.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProtocoloTest extends PruebaProtocoloBase {

    @Test
    @DisplayName("Envía y recibe varios mensajes en orden por el mismo flujo")
    void conservaElOrdenDeLosMensajes() throws IOException, ClassNotFoundException {
        Solicitud primera = Solicitud.conId("1", Operacion.LISTAR_CATEGORIAS);
        Solicitud segunda = Solicitud.conId("2", Operacion.INICIAR_SESION, "222", "clave");
        Notificacion tercera = Notificacion.nuevaReserva(ReservaDePrueba.crear());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = Protocolo.crearSalida(bytes)) {
            Protocolo.enviar(salida, primera);
            Protocolo.enviar(salida, segunda);
            Protocolo.enviar(salida, tercera);
        }
        try (ObjectInputStream entrada = Protocolo.crearEntrada(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(primera, Protocolo.recibir(entrada));
            assertEquals(segunda, Protocolo.recibir(entrada));
            assertEquals(tercera, Protocolo.recibir(entrada));
        }
    }

    @Test
    @DisplayName("Enviar dos veces el mismo objeto produce dos mensajes completos")
    void repiteElMismoObjetoTrasReset() throws IOException, ClassNotFoundException {
        Solicitud solicitud = Solicitud.conId("mismo", Operacion.LISTAR_RECURSOS);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = Protocolo.crearSalida(bytes)) {
            Protocolo.enviar(salida, solicitud);
            Protocolo.enviar(salida, solicitud);
        }
        try (ObjectInputStream entrada = Protocolo.crearEntrada(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(solicitud, Protocolo.recibir(entrada));
            assertEquals(solicitud, Protocolo.recibir(entrada));
        }
    }

    @Test
    @DisplayName("Rechaza clases que no pertenecen al protocolo")
    void rechazaClasesNoPermitidas() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) {
            salida.writeObject(new File("secreto.txt"));
        }

        try (ObjectInputStream entrada = Protocolo.crearEntrada(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            assertThrows(IOException.class, () -> Protocolo.recibir(entrada));
        }
    }

    @Test
    @DisplayName("Genera identificadores de correlación únicos y no vacíos")
    void generaIdentificadoresUnicos() {
        Set<String> ids = IntStream.range(0, 200)
                .mapToObj(i -> Protocolo.nuevoId())
                .collect(Collectors.toSet());

        assertEquals(200, ids.size());
        assertTrue(ids.stream().noneMatch(String::isBlank));
    }

    @Test
    @DisplayName("El límite de bytes se reinicia después de cada mensaje")
    void reiniciaElLimiteDeBytesPorMensaje() throws IOException, ClassNotFoundException {
        String relleno = "x".repeat(7_000);
        Solicitud solicitud = Solicitud.conId("corr",
                Operacion.EXISTE_DESCRIPCION_CATEGORIA, relleno);
        byte[] mensaje = serializar(solicitud);
        long limite = mensaje.length + 4_096L;
        assertTrue(2L * mensaje.length > limite,
                "Dos mensajes juntos deben superar el límite de la prueba.");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = Protocolo.crearSalida(bytes)) {
            Protocolo.enviar(salida, solicitud);
            Protocolo.enviar(salida, solicitud);
        }
        try (ObjectInputStream entrada = Protocolo.crearEntrada(
                new ByteArrayInputStream(bytes.toByteArray()), limite)) {
            assertEquals(solicitud, Protocolo.recibir(entrada));
            assertEquals(solicitud, Protocolo.recibir(entrada));
        }
    }

    @Test
    @DisplayName("Rechaza un mensaje que supera el límite de bytes")
    void rechazaMensajeDemasiadoGrande() throws IOException {
        Solicitud solicitud = Solicitud.conId("corr",
                Operacion.EXISTE_DESCRIPCION_CATEGORIA, "x".repeat(7_000));
        byte[] mensaje = serializar(solicitud);

        try (ObjectInputStream entrada = Protocolo.crearEntrada(
                new ByteArrayInputStream(mensaje), mensaje.length - 100L)) {
            assertThrows(IOException.class, () -> Protocolo.recibir(entrada));
        }
    }

    private static byte[] serializar(Object mensaje) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = Protocolo.crearSalida(bytes)) {
            Protocolo.enviar(salida, mensaje);
        }
        return bytes.toByteArray();
    }
}
