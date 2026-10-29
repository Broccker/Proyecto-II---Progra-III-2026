package com.sistema.common.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sistema.common.servicio.Service;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OperacionTest {

    @Test
    @DisplayName("Cada método de Service tiene exactamente una operación")
    void mapeaTodosLosMetodosDelService() {
        Set<String> esperadas = Arrays.stream(Service.class.getMethods())
                .filter(metodo -> metodo.getDeclaringClass() != Object.class)
                .map(Method::getName)
                .map(OperacionTest::aConstante)
                .collect(Collectors.toSet());
        Set<String> reales = Arrays.stream(Operacion.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(esperadas, reales);
    }

    @Test
    @DisplayName("Cada operación apunta al método correcto y a su cantidad de parámetros")
    void apuntaAlMetodoConSuAridad() {
        Map<String, Method> metodos = Arrays.stream(Service.class.getMethods())
                .collect(Collectors.toMap(Method::getName, metodo -> metodo));

        for (Operacion operacion : Operacion.values()) {
            Method metodo = metodos.get(operacion.metodo());
            assertNotNull(metodo, "No existe el método " + operacion.metodo());
            assertEquals(metodo.getParameterCount(), operacion.cantidadParametros(),
                    "Aridad incorrecta en " + operacion);
            assertEquals(aConstante(operacion.metodo()), operacion.name(),
                    "El nombre de la constante no corresponde al método");
        }
    }

    /**
     * Convierte {@code listarReservasDe} en {@code LISTAR_RESERVAS_DE}.
     */
    static String aConstante(String metodo) {
        StringBuilder resultado = new StringBuilder(metodo.length() + 4);
        for (int i = 0; i < metodo.length(); i++) {
            char caracter = metodo.charAt(i);
            if (Character.isUpperCase(caracter) && resultado.length() > 0) {
                resultado.append('_');
            }
            resultado.append(Character.toUpperCase(caracter));
        }
        return resultado.toString();
    }
}
