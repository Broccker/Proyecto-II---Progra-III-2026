package com.sistema.common.protocolo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.dto.CeldaCalendario;
import com.sistema.common.dto.ConteoCategoria;
import com.sistema.common.dto.ConteoSemana;
import com.sistema.common.dto.DatosReservaSugerida;
import com.sistema.common.dto.MatrizActividades;
import com.sistema.common.dto.MatrizCalendarizacion;
import com.sistema.common.modelo.Administrador;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.EstadoReserva;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import com.sistema.common.modelo.Rol;
import com.sistema.common.modelo.Usuario;
import com.sistema.common.servicio.Service;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SerializacionModelosTest {

    @Test
    @DisplayName("Todos los modelos y DTO del protocolo son Serializable")
    void modelosSerializables() {
        List<Class<?>> tipos = List.of(
                Usuario.class, Administrador.class, Funcionario.class,
                CategoriaRecurso.class, Recurso.class, Reserva.class,
                Rol.class, EstadoReserva.class,
                CeldaCalendario.class, ConteoCategoria.class, ConteoSemana.class,
                DatosReservaSugerida.class, MatrizActividades.class, MatrizCalendarizacion.class,
                Solicitud.class, Respuesta.class, Notificacion.class, ErrorRemoto.class,
                CargaNuevaReserva.class, CargaNotificacion.class,
                Operacion.class, TipoNotificacion.class, CodigoError.class);

        for (Class<?> tipo : tipos) {
            assertTrue(Serializable.class.isAssignableFrom(tipo),
                    tipo.getName() + " debe implementar Serializable");
        }
    }

    @Test
    @DisplayName("Los parámetros y resultados de Service son serializables o tipos básicos")
    void firmasDelServiceSonSerializables() {
        for (Method metodo : Service.class.getMethods()) {
            comprobar(metodo.getReturnType(), metodo);
            for (Class<?> parametro : metodo.getParameterTypes()) {
                comprobar(parametro, metodo);
            }
        }
    }

    private static void comprobar(Class<?> tipo, Method metodo) {
        if (tipo.isPrimitive() || tipo == void.class || List.class.isAssignableFrom(tipo)) {
            return;
        }
        assertTrue(Serializable.class.isAssignableFrom(tipo),
                "El tipo " + tipo.getName() + " del método " + metodo.getName()
                        + " debe ser Serializable");
    }
}
