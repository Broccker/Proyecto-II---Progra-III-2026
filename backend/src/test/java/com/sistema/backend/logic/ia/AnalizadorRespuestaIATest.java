package com.sistema.backend.logic.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sistema.common.excepciones.ValidacionException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnalizadorRespuestaIATest {

    private final AnalizadorRespuestaIA analizador = new AnalizadorRespuestaIA();

    @Test
    @DisplayName("Analiza un objeto JSON directo")
    void analizaJsonDirecto() {
        String cuerpo = """
                {
                  "actividad": "Reunión de equipo",
                  "fecha": "2026-03-02",
                  "horaInicio": "09:00",
                  "horaFin": "11:00",
                  "categorias": ["Salas", " Proyectores ", ""]
                }
                """;

        PropuestaIA propuesta = analizador.analizar(cuerpo);

        assertEquals("Reunión de equipo", propuesta.actividad());
        assertEquals(LocalDate.of(2026, 3, 2), propuesta.fecha());
        assertEquals(LocalTime.of(9, 0), propuesta.horaInicio());
        assertEquals(LocalTime.of(11, 0), propuesta.horaFin());
        assertEquals(List.of("Salas", "Proyectores"), propuesta.categorias());
    }

    @Test
    @DisplayName("Analiza la envoltura choices[0].message.content de una API de chat")
    void analizaEnvolturaDeChat() {
        String contenido = """
                {"actividad":"Clase","fecha":"2026-03-03","horaInicio":"10:00",
                 "horaFin":"12:00","categorias":["Salas"]}
                """;
        String cuerpo = new JSONObject()
                .put("choices", new JSONArray().put(new JSONObject()
                        .put("message", new JSONObject().put("content", contenido))))
                .toString();

        PropuestaIA propuesta = analizador.analizar(cuerpo);

        assertEquals("Clase", propuesta.actividad());
        assertEquals(LocalDate.of(2026, 3, 3), propuesta.fecha());
        assertEquals(List.of("Salas"), propuesta.categorias());
    }

    @Test
    @DisplayName("Encuentra el JSON aunque el modelo agregue texto alrededor")
    void analizaJsonConTextoSobreagregado() {
        String cuerpo = "Claro, aquí está el resultado:\n"
                + "{\"actividad\":\"Charla\",\"fecha\":\"2026-03-04\","
                + "\"horaInicio\":\"08:00\",\"horaFin\":\"09:00\",\"categorias\":[]}\n"
                + "Espero que te sirva.";

        PropuestaIA propuesta = analizador.analizar(cuerpo);

        assertEquals("Charla", propuesta.actividad());
        assertEquals(List.of(), propuesta.categorias());
    }

    @Test
    @DisplayName("Rechaza respuestas vacías, sin JSON o sin actividad")
    void rechazaRespuestasInvalidas() {
        assertThrows(ValidacionException.class, () -> analizador.analizar(null));
        assertThrows(ValidacionException.class, () -> analizador.analizar("   "));
        assertThrows(ValidacionException.class, () -> analizador.analizar("sin json"));
        assertThrows(ValidacionException.class, () -> analizador.analizar(
                "{\"fecha\":\"2026-03-02\",\"horaInicio\":\"09:00\"}"));
    }

    @Test
    @DisplayName("Rechaza fechas, horas y rangos inválidos")
    void rechazaDatosInvalidos() {
        assertThrows(ValidacionException.class, () -> analizador.analizar("""
                {"actividad":"A","fecha":"2026-13-40","horaInicio":"09:00",
                 "horaFin":"10:00","categorias":[]}
                """));
        assertThrows(ValidacionException.class, () -> analizador.analizar("""
                {"actividad":"A","fecha":"2026-03-02","horaInicio":"25:00",
                 "horaFin":"26:00","categorias":[]}
                """));
        assertThrows(ValidacionException.class, () -> analizador.analizar("""
                {"actividad":"A","fecha":"2026-03-02","horaInicio":"11:00",
                 "horaFin":"09:00","categorias":[]}
                """));
    }
}
