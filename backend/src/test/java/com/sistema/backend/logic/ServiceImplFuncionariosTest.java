package com.sistema.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Administrador;
import com.sistema.common.modelo.EstadoReserva;
import com.sistema.common.modelo.Funcionario;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mindrot.jbcrypt.BCrypt;

@ExtendWith(MockitoExtension.class)
class ServiceImplFuncionariosTest extends PruebaServicioBase {

    @Test
    @DisplayName("Da de alta un funcionario con su identificador como clave inicial")
    void daDeAltaConClaveInicial() {
        when(usuarioDAO.existeId("555")).thenReturn(false);

        servicio.altaFuncionario(" 555 ", "Carlos Mora", "77778888");

        ArgumentCaptor<Funcionario> funcionario = ArgumentCaptor.forClass(Funcionario.class);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(usuarioDAO).insertar(funcionario.capture(), hash.capture());

        assertEquals("555", funcionario.getValue().getId());
        assertEquals("Carlos Mora", funcionario.getValue().getNombre());
        assertEquals("77778888", funcionario.getValue().getTelefono());
        assertTrue(BCrypt.checkpw("555", hash.getValue()));
    }

    @Test
    @DisplayName("Rechaza un identificador ya usado, incluso por un administrador")
    void rechazaIdentificadorDuplicado() {
        when(usuarioDAO.existeId("111")).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> servicio.altaFuncionario("111", "Carlos Mora", "77778888"));
        verify(usuarioDAO, never()).insertar(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("Valida nombre y teléfono antes de tocar la base")
    void validaDatosObligatorios() {
        assertThrows(ValidacionException.class,
                () -> servicio.altaFuncionario("555", " ", "77778888"));
        assertThrows(ValidacionException.class,
                () -> servicio.altaFuncionario("555", "Carlos", "abc"));
        assertThrows(ValidacionException.class,
                () -> servicio.altaFuncionario(" ", "Carlos", "77778888"));
        verify(usuarioDAO, never()).insertar(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("Rechaza un nombre de más de 100 caracteres")
    void rechazaNombreDemasiadoLargo() {
        assertThrows(ValidacionException.class,
                () -> servicio.altaFuncionario("555", "N".repeat(101), "77778888"));
    }

    @Test
    @DisplayName("Modifica nombre y teléfono de un funcionario existente")
    void modificaFuncionario() {
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(funcionario("222")));

        servicio.modificarFuncionario("222", "Ana María Pérez", "60001111");

        ArgumentCaptor<Funcionario> capturado = ArgumentCaptor.forClass(Funcionario.class);
        verify(usuarioDAO).actualizarFuncionario(capturado.capture());
        assertEquals("Ana María Pérez", capturado.getValue().getNombre());
        assertEquals("60001111", capturado.getValue().getTelefono());
    }

    @Test
    @DisplayName("No modifica a un usuario inexistente o que no es funcionario")
    void rechazaModificarUsuarioNoFuncionario() {
        when(usuarioDAO.buscarPorId("111")).thenReturn(Optional.of(new Administrador("111")));

        assertThrows(NoEncontradoException.class,
                () -> servicio.modificarFuncionario("111", "Otro", "77778888"));
        verify(usuarioDAO, never()).actualizarFuncionario(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("No elimina un funcionario con reservas registradas")
    void rechazaEliminarConReservas() {
        Funcionario ana = funcionario("222");
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(ana));
        when(reservaDAO.listarPorFuncionario("222")).thenReturn(List.of(
                reserva("RES-000001", ana, HOY, "09:00", "10:00", EstadoReserva.CANCELADA,
                        recurso("1001", categoria("CAT-000001", "Proyectores"), "Proyector"))));

        assertThrows(ConflictoException.class, () -> servicio.eliminarFuncionario("222"));
        verify(usuarioDAO, never()).eliminar("222");
    }

    @Test
    @DisplayName("Elimina un funcionario sin reservas")
    void eliminaFuncionarioSinReservas() {
        when(usuarioDAO.buscarPorId("222")).thenReturn(Optional.of(funcionario("222")));
        when(reservaDAO.listarPorFuncionario("222")).thenReturn(List.of());

        servicio.eliminarFuncionario("222");

        verify(usuarioDAO).eliminar("222");
    }

    @Test
    @DisplayName("Lista y busca funcionarios delegando en el DAO")
    void listaYBuscaFuncionarios() {
        List<Funcionario> esperados = List.of(funcionario("222"));
        when(usuarioDAO.listarFuncionarios()).thenReturn(esperados);
        when(usuarioDAO.buscarFuncionarios("Ana")).thenReturn(esperados);

        assertEquals(esperados, servicio.listarFuncionarios());
        assertEquals(esperados, servicio.buscarFuncionarios("Ana"));
    }
}
