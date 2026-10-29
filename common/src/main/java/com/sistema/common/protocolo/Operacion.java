package com.sistema.common.protocolo;

/**
 * Operaciones que el frontend puede solicitar al backend.
 *
 * <p>Cada constante corresponde <strong>1:1</strong> con un método de
 * {@link com.sistema.common.servicio.Service}; el nombre del método y la
 * cantidad de parámetros se guardan aquí para que el servidor pueda validar
 * las solicitudes y para que una prueba compruebe que no falte ninguna.</p>
 */
public enum Operacion {

    INICIAR_SESION("iniciarSesion", 2),
    CAMBIAR_CLAVE("cambiarClave", 4),

    ALTA_FUNCIONARIO("altaFuncionario", 3),
    MODIFICAR_FUNCIONARIO("modificarFuncionario", 3),
    ELIMINAR_FUNCIONARIO("eliminarFuncionario", 1),
    LISTAR_FUNCIONARIOS("listarFuncionarios", 0),
    BUSCAR_FUNCIONARIOS("buscarFuncionarios", 1),

    ALTA_CATEGORIA("altaCategoria", 1),
    MODIFICAR_CATEGORIA("modificarCategoria", 3),
    ELIMINAR_CATEGORIA("eliminarCategoria", 1),
    LISTAR_CATEGORIAS("listarCategorias", 0),
    BUSCAR_CATEGORIAS("buscarCategorias", 1),
    EXISTE_DESCRIPCION_CATEGORIA("existeDescripcionCategoria", 1),

    ALTA_RECURSO("altaRecurso", 3),
    MODIFICAR_RECURSO("modificarRecurso", 4),
    ELIMINAR_RECURSO("eliminarRecurso", 1),
    LISTAR_RECURSOS("listarRecursos", 0),
    BUSCAR_RECURSOS("buscarRecursos", 1),
    FILTRAR_RECURSOS("filtrarRecursos", 2),

    CREAR_RESERVA("crearReserva", 6),
    CANCELAR_RESERVA("cancelarReserva", 2),
    LISTAR_RESERVAS_DE("listarReservasDe", 1),
    LISTAR_TODAS_LAS_RESERVAS("listarTodasLasReservas", 0),
    LISTAR_RESERVAS_ACTIVAS_ENTRE("listarReservasActivasEntre", 2),

    CONSTRUIR_MATRIZ_ACTIVIDADES("construirMatrizActividades", 1),
    CONSTRUIR_MATRIZ_CALENDARIZACION("construirMatrizCalendarizacion", 2),

    CATEGORIAS_RESERVADAS("categoriasReservadas", 2),
    ACTIVIDADES_POR_SEMANA("actividadesPorSemana", 2),

    EXTRAER_DATOS_RESERVA("extraerDatosReserva", 1);

    private final String metodo;
    private final int cantidadParametros;

    Operacion(String metodo, int cantidadParametros) {
        this.metodo = metodo;
        this.cantidadParametros = cantidadParametros;
    }

    /**
     * Nombre del método de {@code Service} que atiende esta operación.
     */
    public String metodo() {
        return metodo;
    }

    /**
     * Cantidad exacta de parámetros que debe traer la {@link Solicitud}.
     */
    public int cantidadParametros() {
        return cantidadParametros;
    }
}
