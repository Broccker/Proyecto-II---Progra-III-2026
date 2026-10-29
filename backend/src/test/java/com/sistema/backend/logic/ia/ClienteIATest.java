package com.sistema.backend.logic.ia;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.excepciones.ConfiguracionException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.CategoriaRecurso;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClienteIATest {

    private static final List<CategoriaRecurso> CATEGORIAS = List.of(
            new CategoriaRecurso("CAT-000001", "Salas"),
            new CategoriaRecurso("CAT-000002", "Proyectores"));

    @Test
    @DisplayName("Construye la petición con el modelo, la frase y las categorías disponibles")
    void construyePeticion() {
        ClienteIA cliente = new ClienteIA("https://ejemplo.test/v1/chat", "secreta", "modelo-x");

        JSONObject peticion = new JSONObject(
                cliente.cuerpoPeticion("Reserve una sala", CATEGORIAS));
        JSONArray mensajes = peticion.getJSONArray("messages");

        assertTrue(peticion.getString("model").equals("modelo-x"));
        assertTrue(mensajes.getJSONObject(0).getString("content").contains("Salas"));
        assertTrue(mensajes.getJSONObject(0).getString("content").contains("Proyectores"));
        assertTrue(mensajes.getJSONObject(1).getString("content").contains("Reserve una sala"));
    }

    @Test
    @DisplayName("Usa el modelo por defecto y no menciona categorías si no hay")
    void usaValoresPorDefecto() {
        ClienteIA cliente = new ClienteIA("https://ejemplo.test/v1/chat", "", "");

        JSONObject peticion = new JSONObject(cliente.cuerpoPeticion("Reserve algo", List.of()));

        assertTrue(peticion.getString("model").equals("gpt-4o-mini"));
        assertTrue(!peticion.getJSONArray("messages").getJSONObject(0)
                .getString("content").contains("categorías disponibles"));
    }

    @Test
    @DisplayName("Falla con un mensaje claro si no hay URL o la frase está vacía")
    void validaConfiguracionYEntrada() {
        ClienteIA sinUrl = new ClienteIA(" ", "", "modelo");

        assertThrows(ValidacionException.class,
                () -> sinUrl.extraer("  ", CATEGORIAS));
        assertThrows(ConfiguracionException.class,
                () -> sinUrl.extraer("Reserve una sala", CATEGORIAS));
    }

    @Test
    @DisplayName("Rechaza una URL malformada antes de abrir la conexión")
    void rechazaUrlMalformada() {
        ClienteIA cliente = new ClienteIA("http://[::1", "", "modelo");

        assertThrows(ConfiguracionException.class,
                () -> cliente.extraer("Reserve una sala", CATEGORIAS));
    }
}
