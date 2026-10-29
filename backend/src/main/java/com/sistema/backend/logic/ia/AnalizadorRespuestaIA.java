package com.sistema.backend.logic.ia;

import com.sistema.common.excepciones.ValidacionException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Interpreta la respuesta del proveedor de IA.
 *
 * <p>Acepta tanto el objeto JSON directo como la envoltura habitual de las
 * API de chat ({@code choices[0].message.content}). Si el modelo agrega texto
 * alrededor del JSON, se busca el primer objeto válido.</p>
 */
public class AnalizadorRespuestaIA {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    public PropuestaIA analizar(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new ValidacionException("La respuesta del servicio de IA está vacía.");
        }
        JSONObject json;
        try {
            json = resolverJson(cuerpo);
        } catch (JSONException ex) {
            throw new ValidacionException(
                    "La respuesta del servicio de IA no contiene un JSON válido.", ex);
        }

        String actividad = json.optString("actividad", "").trim();
        String fecha = json.optString("fecha", "").trim();
        String horaInicio = json.optString("horaInicio", "").trim();
        String horaFin = json.optString("horaFin", "").trim();

        if (actividad.isEmpty()) {
            throw new ValidacionException("La respuesta del servicio de IA no incluye la actividad.");
        }

        List<String> categorias = new ArrayList<>();
        JSONArray arreglo = json.optJSONArray("categorias");
        if (arreglo != null) {
            for (int i = 0; i < arreglo.length(); i++) {
                String nombre = arreglo.optString(i, "").trim();
                if (!nombre.isEmpty()) {
                    categorias.add(nombre);
                }
            }
        }
        return new PropuestaIA(actividad, parsearFecha(fecha),
                parsearHora(horaInicio, "hora de inicio"), parsearHora(horaFin, "hora de fin"),
                categorias);
    }

    private JSONObject resolverJson(String cuerpo) {
        for (String candidato : candidatos(cuerpo)) {
            try {
                JSONObject json = new JSONObject(candidato);
                if (json.has("actividad")) {
                    return json;
                }
                if (json.has("choices")) {
                    JSONArray choices = json.optJSONArray("choices");
                    if (choices != null && !choices.isEmpty()) {
                        JSONObject mensaje = choices.getJSONObject(0).optJSONObject("message");
                        String contenido = mensaje == null ? "" : mensaje.optString("content", "");
                        if (!contenido.isBlank()) {
                            return resolverJson(contenido);
                        }
                    }
                }
            } catch (JSONException ignorado) {
                // Se prueba el siguiente objeto encontrado en la respuesta.
            }
        }
        throw new ValidacionException(
                "La respuesta del servicio de IA no contiene un objeto JSON.");
    }

    private static List<String> candidatos(String cuerpo) {
        List<String> resultado = new ArrayList<>();
        for (int i = 0; i < cuerpo.length(); i++) {
            if (cuerpo.charAt(i) != '{') {
                continue;
            }
            int fin = finDeObjeto(cuerpo, i);
            if (fin >= 0) {
                resultado.add(cuerpo.substring(i, fin + 1));
            }
        }
        if (resultado.isEmpty()) {
            throw new ValidacionException(
                    "La respuesta del servicio de IA no contiene un objeto JSON.");
        }
        return resultado;
    }

    private static int finDeObjeto(String texto, int inicio) {
        int profundidad = 0;
        boolean enCadena = false;
        boolean escapado = false;
        for (int i = inicio; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (enCadena) {
                if (escapado) {
                    escapado = false;
                } else if (c == '\\') {
                    escapado = true;
                } else if (c == '"') {
                    enCadena = false;
                }
            } else if (c == '"') {
                enCadena = true;
            } else if (c == '{') {
                profundidad++;
            } else if (c == '}') {
                profundidad--;
                if (profundidad == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static LocalDate parsearFecha(String fecha) {
        try {
            return LocalDate.parse(fecha);
        } catch (DateTimeParseException ex) {
            throw new ValidacionException(
                    "La fecha '" + fecha + "' no está en formato AAAA-MM-DD.", ex);
        }
    }

    private static LocalTime parsearHora(String hora, String campo) {
        try {
            return LocalTime.parse(hora, FORMATO_HORA);
        } catch (DateTimeParseException ex) {
            throw new ValidacionException(
                    "La " + campo + " '" + hora + "' no está en formato HH:mm.", ex);
        }
    }
}
