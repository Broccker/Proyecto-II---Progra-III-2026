package com.sistema.backend.logic.ia;

import com.sistema.common.excepciones.ConfiguracionException;
import com.sistema.common.excepciones.ServicioExternoException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.CategoriaRecurso;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Cliente del proveedor de IA que interpreta frases de reserva.
 *
 * <p>Vive solo en el backend: la URL, la clave y el modelo se leen de
 * {@code backend.properties}, de modo que el frontend nunca recibe
 * credenciales. Si no hay URL configurada, la operación falla con un mensaje
 * claro y el resto del sistema sigue funcionando.</p>
 */
public class ClienteIA {

    private static final String PROMPT_BASE =
            "Eres un asistente de reservas. Extrae de la frase del usuario la información "
            + "de una reserva y responde EXCLUSIVAMENTE con un objeto JSON con las claves: "
            + "actividad (texto), fecha (AAAA-MM-DD), horaInicio (HH:mm), horaFin (HH:mm) "
            + "y categorias (arreglo de nombres de categorías del sistema). "
            + "Si un dato no aparece, usa su valor por defecto para hoy y horario de 08:00 a 17:00.";

    private static final String MODELO_POR_DEFECTO = "gpt-4o-mini";
    private static final Duration TIEMPO_CONEXION = Duration.ofSeconds(20);
    private static final Duration TIEMPO_RESPUESTA = Duration.ofSeconds(60);

    private final String url;
    private final String clave;
    private final String modelo;
    private final AnalizadorRespuestaIA analizador;

    public ClienteIA(String url, String clave, String modelo) {
        this(url, clave, modelo, new AnalizadorRespuestaIA());
    }

    ClienteIA(String url, String clave, String modelo, AnalizadorRespuestaIA analizador) {
        this.url = url == null ? "" : url.trim();
        this.clave = clave == null ? "" : clave.trim();
        this.modelo = modelo == null || modelo.isBlank() ? MODELO_POR_DEFECTO : modelo.trim();
        this.analizador = analizador == null ? new AnalizadorRespuestaIA() : analizador;
    }

    /**
     * Envía la frase al proveedor y devuelve los datos sin resolver categorías.
     *
     * @param frase      texto libre escrito por el usuario
     * @param categorias categorías actuales, para indicarle al modelo cuáles usar
     */
    public PropuestaIA extraer(String frase, List<CategoriaRecurso> categorias) {
        if (frase == null || frase.isBlank()) {
            throw new ValidacionException("Ingrese una frase para extraer la reserva.");
        }
        if (url.isBlank()) {
            throw new ConfiguracionException(
                    "No se configuró la URL del servicio de IA (llm.api.url).");
        }

        URI destino;
        try {
            destino = URI.create(url);
        } catch (IllegalArgumentException ex) {
            throw new ConfiguracionException(
                    "La URL del servicio de IA no es válida: " + url, ex);
        }

        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(TIEMPO_CONEXION)
                .build();
        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(destino)
                .timeout(TIEMPO_RESPUESTA)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        cuerpoPeticion(frase, categorias)));
        if (!clave.isBlank()) {
            peticion.header("Authorization", "Bearer " + clave);
        }

        try {
            HttpResponse<String> respuesta = cliente.send(
                    peticion.build(), HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() >= 300) {
                throw new ServicioExternoException(
                        "El servicio de IA respondió con error HTTP "
                                + respuesta.statusCode() + ".");
            }
            return analizador.analizar(respuesta.body());
        } catch (IOException ex) {
            throw new ServicioExternoException(
                    "No se pudo conectar con el servicio de IA: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServicioExternoException(
                    "La consulta al servicio de IA fue interrumpida.", ex);
        }
    }

    String construirPromptSistema(List<CategoriaRecurso> categorias) {
        if (categorias == null || categorias.isEmpty()) {
            return PROMPT_BASE;
        }
        String nombres = categorias.stream()
                .map(CategoriaRecurso::getDescripcion)
                .collect(Collectors.joining(", "));
        return PROMPT_BASE + " Las categorías disponibles del sistema son: " + nombres
                + ". Nombra SOLO categorías de esa lista, con la misma redacción.";
    }

    String cuerpoPeticion(String frase, List<CategoriaRecurso> categorias) {
        JSONArray mensajes = new JSONArray()
                .put(new JSONObject().put("role", "system")
                        .put("content", construirPromptSistema(categorias)))
                .put(new JSONObject().put("role", "user").put("content", frase.trim()));
        return new JSONObject()
                .put("model", modelo)
                .put("messages", mensajes)
                .toString();
    }
}
