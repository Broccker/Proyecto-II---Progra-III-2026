package com.sistema.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistema.backend.logic.ia.PropuestaIA;
import com.sistema.common.dto.DatosReservaSugerida;
import com.sistema.common.excepciones.ServicioExternoException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.CategoriaRecurso;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceImplIATest extends PruebaServicioBase {

    @Test
    @DisplayName("Traduce los nombres de categoría de la IA a las categorías reales")
    void resuelveCategoriasSugeridas() {
        CategoriaRecurso salas = categoria("CAT-000002", "Salas");
        CategoriaRecurso proyectores = categoria("CAT-000001", "Proyectores");
        when(categoriaDAO.listarTodas()).thenReturn(List.of(proyectores, salas));
        when(clienteIA.extraer("Necesito una sala con proyector el lunes de 9 a 11",
                List.of(proyectores, salas)))
                .thenReturn(new PropuestaIA("Reunión de equipo", HOY,
                        LocalTime.of(9, 0), LocalTime.of(11, 0),
                        List.of("salas", "Proyectores", "Impresoras")));

        DatosReservaSugerida datos = servicio.extraerDatosReserva(
                "Necesito una sala con proyector el lunes de 9 a 11");

        assertEquals("Reunión de equipo", datos.actividad());
        assertEquals(HOY, datos.fecha());
        assertEquals(2, datos.categorias().size());
        assertEquals(List.of("Impresoras"), datos.categoriasNoEncontradas());
    }

    @Test
    @DisplayName("Avisa cuando la IA no reconoce ninguna categoría del sistema")
    void informaCategoriasNoEncontradas() {
        CategoriaRecurso salas = categoria("CAT-000002", "Salas");
        when(categoriaDAO.listarTodas()).thenReturn(List.of(salas));
        when(clienteIA.extraer("Reserve algo", List.of(salas)))
                .thenReturn(new PropuestaIA("Actividad", HOY, LocalTime.of(8, 0),
                        LocalTime.of(9, 0), List.of("Drones")));

        DatosReservaSugerida datos = servicio.extraerDatosReserva("Reserve algo");

        assertTrue(datos.categorias().isEmpty());
        assertEquals(List.of("Drones"), datos.categoriasNoEncontradas());
    }

    @Test
    @DisplayName("Rechaza una frase vacía sin llamar al proveedor")
    void rechazaFraseVacia() {
        assertThrows(ValidacionException.class, () -> servicio.extraerDatosReserva(" "));
        verifyNoInteractions(clienteIA, categoriaDAO);
    }

    @Test
    @DisplayName("Propaga el fallo del servicio externo como excepción de dominio")
    void propagaFalloDelServicio() {
        when(categoriaDAO.listarTodas()).thenReturn(List.of());
        when(clienteIA.extraer("Reserve una sala", List.of()))
                .thenThrow(new ServicioExternoException("El servicio de IA no responde."));

        ServicioExternoException error = assertThrows(ServicioExternoException.class,
                () -> servicio.extraerDatosReserva("Reserve una sala"));

        assertEquals("El servicio de IA no responde.", error.getMessage());
    }
}
