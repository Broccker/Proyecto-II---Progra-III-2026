package com.sistema.frontend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.protocolo.CargaNuevaReserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlertaTest {

    private static Alerta alerta() {
        return new Alerta("RES-000001", "Ana Pérez", "Reunión de equipo",
                LocalDate.of(2026, 11, 2), LocalTime.of(9, 0), LocalTime.of(11, 0),
                List.of(new Recurso("1001", new CategoriaRecurso("CAT-000001", "Proyectores"),
                                "Proyector Epson"),
                        new Recurso("1002", new CategoriaRecurso("CAT-000002", "Salas"),
                                "Sala 3")));
    }

    @Test
    @DisplayName("La columna Actividad usa el formato Encargado - Nombre actividad")
    void textoDeActividad() {
        assertEquals("Ana Pérez - Reunión de equipo", alerta().textoActividad());
    }

    @Test
    @DisplayName("Los recursos se muestran separados por coma")
    void textoDeRecursos() {
        assertEquals("Proyector Epson, Sala 3", alerta().textoRecursos());
    }

    @Test
    @DisplayName("Sin recursos se informa explícitamente")
    void sinRecursos() {
        Alerta sinRecursos = new Alerta("RES-000002", "Luis", "Charla",
                LocalDate.of(2026, 11, 3), LocalTime.of(8, 0), LocalTime.of(9, 0),
                List.of());
        assertEquals("Sin recursos", sinRecursos.textoRecursos());
    }

    @Test
    @DisplayName("Se construye desde la carga de la notificación")
    void seConstruyeDesdeLaCarga() {
        Alerta original = alerta();
        Alerta copia = Alerta.desde(new CargaNuevaReserva("RES-000001", "Ana Pérez",
                "Reunión de equipo", original.fecha(), original.horaInicio(),
                original.horaFin(), original.recursos()));

        assertEquals(original, copia);
    }

    @Test
    @DisplayName("Los datos de la reserva son obligatorios")
    void exigeLosDatos() {
        assertThrows(NullPointerException.class,
                () -> new Alerta("RES-000001", null, "Actividad",
                        LocalDate.now(), LocalTime.NOON, LocalTime.NOON.plusHours(1),
                        List.of()));
    }
}
