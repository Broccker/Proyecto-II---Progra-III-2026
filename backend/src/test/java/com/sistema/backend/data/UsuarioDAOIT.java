package com.sistema.backend.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistema.common.excepciones.ConflictoException;
import com.sistema.common.excepciones.NoEncontradoException;
import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.modelo.Administrador;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Funcionario;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Rol;
import com.sistema.common.modelo.Usuario;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

class UsuarioDAOIT extends PruebaBDBase {

    private UsuarioDAO dao() {
        return new UsuarioDAO(fuente());
    }

    @Test
    @DisplayName("Inserta un administrador y lo recupera sin clave")
    void insertaYRecuperaAdministrador() {
        dao().insertar(new Administrador("111"), CLAVE_HASH);

        Usuario usuario = dao().buscarPorId("111").orElseThrow();

        assertInstanceOf(Administrador.class, usuario);
        assertEquals(Rol.ADMINISTRADOR, usuario.getRol());
        assertTrue(dao().buscarClaveHash("111").orElseThrow().startsWith("$2"));
    }

    @Test
    @DisplayName("Inserta un funcionario con nombre y teléfono")
    void insertaYRecuperaFuncionario() {
        dao().insertar(new Funcionario("222", "Ana Pérez", "88881111"), CLAVE_HASH);

        Funcionario funcionario = (Funcionario) dao().buscarPorId("222").orElseThrow();

        assertEquals("Ana Pérez", funcionario.getNombre());
        assertEquals("88881111", funcionario.getTelefono());
        assertEquals(Rol.FUNCIONARIO, funcionario.getRol());
    }

    @Test
    @DisplayName("Lista solo funcionarios, ordenados por nombre")
    void listaSoloFuncionarios() {
        dao().insertar(new Administrador("111"), CLAVE_HASH);
        dao().insertar(new Funcionario("444", "Pedro Pérez", "88884444"), CLAVE_HASH);
        dao().insertar(new Funcionario("222", "Ana Pérez", "88881111"), CLAVE_HASH);

        List<Funcionario> funcionarios = dao().listarFuncionarios();

        assertEquals(2, funcionarios.size());
        assertEquals(List.of("222", "444"),
                funcionarios.stream().map(Funcionario::getId).toList());
        assertEquals(2, dao().contarFuncionarios());
    }

    @Test
    @DisplayName("Busca funcionarios por id, nombre o teléfono")
    void buscaFuncionariosPorVariosCriterios() {
        dao().insertar(new Funcionario("222", "Ana Pérez", "88881111"), CLAVE_HASH);
        dao().insertar(new Funcionario("333", "Luis Rojas", "88882222"), CLAVE_HASH);

        assertEquals(1, dao().buscarFuncionarios("333").size());
        assertEquals(1, dao().buscarFuncionarios("Ana").size());
        assertEquals(1, dao().buscarFuncionarios("88882222").size());
        assertEquals(2, dao().buscarFuncionarios("").size());
        assertEquals(0, dao().buscarFuncionarios("no-existe").size());
    }

    @Test
    @DisplayName("Actualiza los datos de un funcionario")
    void actualizaFuncionario() {
        dao().insertar(new Funcionario("222", "Ana Pérez", "88881111"), CLAVE_HASH);

        dao().actualizarFuncionario(new Funcionario("222", "Ana María Pérez", "77770000"));

        Funcionario actualizado = (Funcionario) dao().buscarPorId("222").orElseThrow();
        assertEquals("Ana María Pérez", actualizado.getNombre());
        assertEquals("77770000", actualizado.getTelefono());
    }

    @Test
    @DisplayName("Cambia la clave y guarda un hash BCrypt verificable")
    void actualizaClaveConHashVerificable() {
        dao().insertar(new Funcionario("222", "Ana Pérez", "88881111"), CLAVE_HASH);

        String nuevoHash = BCrypt.hashpw("nueva-clave", BCrypt.gensalt(4));
        dao().actualizarClave("222", nuevoHash);

        String guardado = dao().buscarClaveHash("222").orElseThrow();
        assertTrue(BCrypt.checkpw("nueva-clave", guardado));
        assertFalse(BCrypt.checkpw("clave", guardado));
    }

    @Test
    @DisplayName("Rechaza guardar una clave en texto plano")
    void rechazaClaveEnTextoPlano() {
        assertThrows(ValidacionException.class,
                () -> dao().insertar(new Administrador("111"), "111"));
        assertEquals(0, contar("usuarios"));
    }

    @Test
    @DisplayName("Rechaza un identificador duplicado")
    void rechazaIdentificadorDuplicado() {
        dao().insertar(new Administrador("111"), CLAVE_HASH);

        assertThrows(ConflictoException.class,
                () -> dao().insertar(new Administrador("111"), CLAVE_HASH));
        assertEquals(1, contar("usuarios"));
    }

    @Test
    @DisplayName("Elimina un usuario sin datos relacionados")
    void eliminaUsuario() {
        dao().insertar(new Funcionario("222", "Ana Pérez", "88881111"), CLAVE_HASH);

        dao().eliminar("222");

        assertTrue(dao().buscarPorId("222").isEmpty());
        assertEquals(0, contar("usuarios"));
    }

    @Test
    @DisplayName("Fallar al actualizar o eliminar un usuario inexistente")
    void operacionesSobreInexistente() {
        assertThrows(NoEncontradoException.class,
                () -> dao().actualizarClave("999", CLAVE_HASH));
        assertThrows(NoEncontradoException.class, () -> dao().eliminar("999"));
        assertThrows(NoEncontradoException.class,
                () -> dao().actualizarFuncionario(
                        new Funcionario("999", "Nadie", "88880000")));
    }

    @Test
    @DisplayName("No permite eliminar un funcionario con reservas")
    void noEliminaFuncionarioConReservas() {
        Funcionario funcionario = crearFuncionario("222", "Ana Pérez", "88881111");
        CategoriaRecurso categoria = crearCategoria("CAT-000001", "Salas");
        Recurso recurso = crearRecurso("2001", categoria, "Sala A");
        insertarReserva("RES-000001", funcionario, LocalDate.of(2026, 3, 2),
                "09:00", "10:00", recurso);

        assertThrows(ConflictoException.class, () -> dao().eliminar("222"));
        assertTrue(dao().buscarPorId("222").isPresent());
    }
}
