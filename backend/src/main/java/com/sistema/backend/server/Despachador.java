package com.sistema.backend.server;

import com.sistema.common.excepciones.ValidacionException;
import com.sistema.common.protocolo.Solicitud;
import com.sistema.common.servicio.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Traduce una {@link Solicitud} del protocolo a la llamada correspondiente de
 * {@link Service}.
 *
 * <p>El {@code switch} es exhaustivo sobre {@link com.sistema.common.protocolo.Operacion}:
 * si en el futuro se agrega una operación, el compilador obliga a atenderla
 * aquí. La cantidad de parámetros ya fue validada por {@code Solicitud}; esta
 * clase comprueba además que cada valor tenga el tipo que espera el método.</p>
 */
public final class Despachador {

    private final Service servicio;

    public Despachador(Service servicio) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio es obligatorio.");
    }

    /**
     * Ejecuta la operación de la solicitud y devuelve su resultado.
     *
     * @throws ValidacionException si un parámetro no tiene el tipo esperado
     */
    public Object ejecutar(Solicitud solicitud) {
        Objects.requireNonNull(solicitud, "La solicitud es obligatoria.");
        return switch (solicitud.operacion()) {
            // Sesión
            case INICIAR_SESION ->
                    servicio.iniciarSesion(texto(solicitud, 0), texto(solicitud, 1));
            case CAMBIAR_CLAVE -> {
                servicio.cambiarClave(texto(solicitud, 0), texto(solicitud, 1),
                        texto(solicitud, 2), texto(solicitud, 3));
                yield null;
            }

            // Funcionarios
            case ALTA_FUNCIONARIO -> {
                servicio.altaFuncionario(texto(solicitud, 0), texto(solicitud, 1),
                        texto(solicitud, 2));
                yield null;
            }
            case MODIFICAR_FUNCIONARIO -> {
                servicio.modificarFuncionario(texto(solicitud, 0), texto(solicitud, 1),
                        texto(solicitud, 2));
                yield null;
            }
            case ELIMINAR_FUNCIONARIO -> {
                servicio.eliminarFuncionario(texto(solicitud, 0));
                yield null;
            }
            case LISTAR_FUNCIONARIOS -> servicio.listarFuncionarios();
            case BUSCAR_FUNCIONARIOS -> servicio.buscarFuncionarios(texto(solicitud, 0));

            // Categorías
            case ALTA_CATEGORIA -> servicio.altaCategoria(texto(solicitud, 0));
            case MODIFICAR_CATEGORIA -> {
                servicio.modificarCategoria(texto(solicitud, 0), textoONulo(solicitud, 1),
                        texto(solicitud, 2));
                yield null;
            }
            case ELIMINAR_CATEGORIA -> {
                servicio.eliminarCategoria(texto(solicitud, 0));
                yield null;
            }
            case LISTAR_CATEGORIAS -> servicio.listarCategorias();
            case BUSCAR_CATEGORIAS -> servicio.buscarCategorias(texto(solicitud, 0));
            case EXISTE_DESCRIPCION_CATEGORIA ->
                    servicio.existeDescripcionCategoria(texto(solicitud, 0));

            // Recursos
            case ALTA_RECURSO -> {
                servicio.altaRecurso(texto(solicitud, 0), texto(solicitud, 1),
                        texto(solicitud, 2));
                yield null;
            }
            case MODIFICAR_RECURSO -> {
                servicio.modificarRecurso(texto(solicitud, 0), textoONulo(solicitud, 1),
                        texto(solicitud, 2), texto(solicitud, 3));
                yield null;
            }
            case ELIMINAR_RECURSO -> {
                servicio.eliminarRecurso(texto(solicitud, 0));
                yield null;
            }
            case LISTAR_RECURSOS -> servicio.listarRecursos();
            case BUSCAR_RECURSOS -> servicio.buscarRecursos(texto(solicitud, 0));
            case FILTRAR_RECURSOS ->
                    servicio.filtrarRecursos(texto(solicitud, 0), texto(solicitud, 1));

            // Reservas
            case CREAR_RESERVA -> servicio.crearReserva(texto(solicitud, 0),
                    texto(solicitud, 1), fecha(solicitud, 2), hora(solicitud, 3),
                    hora(solicitud, 4), textos(solicitud, 5));
            case CANCELAR_RESERVA -> {
                servicio.cancelarReserva(texto(solicitud, 0), texto(solicitud, 1));
                yield null;
            }
            case LISTAR_RESERVAS_DE -> servicio.listarReservasDe(texto(solicitud, 0));
            case LISTAR_TODAS_LAS_RESERVAS -> servicio.listarTodasLasReservas();
            case LISTAR_RESERVAS_ACTIVAS_ENTRE ->
                    servicio.listarReservasActivasEntre(fecha(solicitud, 0), fecha(solicitud, 1));

            // Actividades y calendarización
            case CONSTRUIR_MATRIZ_ACTIVIDADES ->
                    servicio.construirMatrizActividades(fecha(solicitud, 0));
            case CONSTRUIR_MATRIZ_CALENDARIZACION ->
                    servicio.construirMatrizCalendarizacion(fecha(solicitud, 0),
                            texto(solicitud, 1));

            // Estadísticas
            case CATEGORIAS_RESERVADAS ->
                    servicio.categoriasReservadas(fecha(solicitud, 0), fecha(solicitud, 1));
            case ACTIVIDADES_POR_SEMANA ->
                    servicio.actividadesPorSemana(fecha(solicitud, 0), fecha(solicitud, 1));

            // Extracción con IA
            case EXTRAER_DATOS_RESERVA -> servicio.extraerDatosReserva(texto(solicitud, 0));
        };
    }

    private static String texto(Solicitud solicitud, int indice) {
        Object valor = solicitud.parametro(indice);
        if (valor instanceof String texto) {
            return texto;
        }
        throw tipoInvalido(solicitud, indice, "texto");
    }

    private static String textoONulo(Solicitud solicitud, int indice) {
        Object valor = solicitud.parametro(indice);
        if (valor == null) {
            return null;
        }
        if (valor instanceof String texto) {
            return texto;
        }
        throw tipoInvalido(solicitud, indice, "texto o nulo");
    }

    private static LocalDate fecha(Solicitud solicitud, int indice) {
        Object valor = solicitud.parametro(indice);
        if (valor instanceof LocalDate fecha) {
            return fecha;
        }
        throw tipoInvalido(solicitud, indice, "una fecha");
    }

    private static LocalTime hora(Solicitud solicitud, int indice) {
        Object valor = solicitud.parametro(indice);
        if (valor instanceof LocalTime hora) {
            return hora;
        }
        throw tipoInvalido(solicitud, indice, "una hora");
    }

    private static List<String> textos(Solicitud solicitud, int indice) {
        Object valor = solicitud.parametro(indice);
        if (!(valor instanceof List<?> elementos)) {
            throw tipoInvalido(solicitud, indice, "una lista de textos");
        }
        List<String> textos = new ArrayList<>(elementos.size());
        for (Object elemento : elementos) {
            if (!(elemento instanceof String texto)) {
                throw tipoInvalido(solicitud, indice, "una lista de textos");
            }
            textos.add(texto);
        }
        return textos;
    }

    private static ValidacionException tipoInvalido(Solicitud solicitud, int indice,
                                                    String esperado) {
        return new ValidacionException("El parámetro " + indice + " de "
                + solicitud.operacion() + " debe ser " + esperado + ".");
    }
}
