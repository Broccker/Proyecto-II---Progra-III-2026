package com.sistema.frontend.controller;

import com.sistema.common.dto.DatosReservaSugerida;
import com.sistema.common.modelo.CategoriaRecurso;
import com.sistema.common.modelo.Recurso;
import com.sistema.common.modelo.Reserva;
import com.sistema.common.servicio.Service;
import com.sistema.frontend.model.ModeloReservas;
import com.sistema.frontend.util.Reportes;
import com.sistema.frontend.util.Validaciones;
import com.sistema.frontend.view.VistaMisReservas;
import com.sistema.frontend.view.VistaNuevaReserva;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controlador de las reservas del funcionario autenticado.
 *
 * <p>Coordina dos vistas sobre un mismo {@link ModeloReservas}: el alta con
 * extracción por IA y el historial con cancelación. Las llamadas de red se
 * ejecutan fuera del hilo de eventos.</p>
 */
public final class ControladorReservas {

    private static final String[] ENCABEZADOS =
            {"Id", "Actividad", "Fecha", "Horario", "Recursos", "Estado"};

    private final Service servicio;
    private final ModeloReservas modelo;
    private final VistaNuevaReserva vistaNueva;
    private final VistaMisReservas vistaMisReservas;
    private final String funcionarioId;
    private final EjecutorTareas ejecutor;

    public ControladorReservas(Service servicio, ModeloReservas modelo,
                               VistaNuevaReserva vistaNueva,
                               VistaMisReservas vistaMisReservas,
                               String funcionarioId, EjecutorTareas ejecutor) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio es obligatorio.");
        this.modelo = Objects.requireNonNull(modelo, "El modelo es obligatorio.");
        this.vistaNueva = Objects.requireNonNull(vistaNueva,
                "La vista de alta es obligatoria.");
        this.vistaMisReservas = Objects.requireNonNull(vistaMisReservas,
                "La vista del historial es obligatoria.");
        if (funcionarioId == null || funcionarioId.isBlank()) {
            throw new IllegalArgumentException("El funcionario de la sesión es obligatorio.");
        }
        this.funcionarioId = funcionarioId;
        this.ejecutor = Objects.requireNonNull(ejecutor, "El ejecutor es obligatorio.");

        vistaNueva.alReservar(this::reservar);
        vistaNueva.alExtraer(this::extraer);
        vistaNueva.alLimpiar(vistaNueva::limpiar);
        vistaMisReservas.alCancelar(this::cancelar);
        vistaMisReservas.alImprimir(this::imprimir);
        vistaMisReservas.alActualizar(this::recargarReservas);
        vistaMisReservas.alSeleccionarFila(this::seleccionar);
    }

    /** Carga las categorías para el alta y el historial de reservas. */
    public void cargar() {
        vistaNueva.habilitar(false);
        vistaMisReservas.habilitar(false);
        AtomicReference<Carga> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(new Carga(servicio.listarCategorias(),
                        servicio.listarReservasDe(funcionarioId))),
                () -> {
                    vistaNueva.habilitar(true);
                    vistaMisReservas.habilitar(true);
                    Carga carga = resultado.get();
                    modelo.establecerCategorias(carga.categorias());
                    modelo.establecerReservas(carga.reservas());
                    vistaNueva.actualizar();
                    vistaMisReservas.actualizar();
                },
                error -> {
                    vistaNueva.habilitar(true);
                    vistaMisReservas.habilitar(true);
                    vistaNueva.mostrarError(Errores.mensaje(error));
                });
    }

    private void recargarReservas() {
        vistaMisReservas.habilitar(false);
        AtomicReference<List<Reserva>> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(servicio.listarReservasDe(funcionarioId)),
                () -> {
                    vistaMisReservas.habilitar(true);
                    modelo.establecerReservas(resultado.get());
                    vistaMisReservas.actualizar();
                },
                error -> {
                    vistaMisReservas.habilitar(true);
                    vistaMisReservas.mostrarError(Errores.mensaje(error));
                });
    }

    void reservar() {
        String actividad;
        try {
            actividad = Validaciones.texto(vistaNueva.getActividad(), "La actividad",
                    Validaciones.MAX_ACTIVIDAD);
        } catch (IllegalArgumentException e) {
            vistaNueva.mostrarAdvertencia(e.getMessage());
            return;
        }
        List<String> categorias = vistaNueva.getIdsCategoriasSeleccionadas();
        if (categorias.isEmpty()) {
            vistaNueva.mostrarAdvertencia("Seleccione al menos una categoría.");
            return;
        }

        LocalDate fecha;
        LocalTime inicio;
        LocalTime fin;
        try {
            fecha = vistaNueva.getFecha();
            inicio = vistaNueva.getHoraInicio();
            fin = vistaNueva.getHoraFin();
        } catch (IllegalArgumentException e) {
            vistaNueva.mostrarAdvertencia(e.getMessage());
            return;
        }
        if (fecha.isBefore(LocalDate.now())) {
            vistaNueva.mostrarAdvertencia("La fecha no puede ser anterior a hoy.");
            return;
        }
        if (!fin.isAfter(inicio)) {
            vistaNueva.mostrarAdvertencia(
                    "La hora de fin debe ser posterior a la hora de inicio.");
            return;
        }

        vistaNueva.habilitar(false);
        AtomicReference<Reserva> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(servicio.crearReserva(funcionarioId, actividad, fecha,
                        inicio, fin, categorias)),
                () -> {
                    vistaNueva.habilitar(true);
                    Reserva reserva = resultado.get();
                    vistaNueva.limpiar();
                    vistaNueva.mostrarInformacion(
                            "La reserva " + reserva.getId() + " fue creada.");
                    recargarReservas();
                },
                error -> {
                    vistaNueva.habilitar(true);
                    vistaNueva.mostrarError(Errores.mensaje(error));
                });
    }

    void extraer() {
        String frase;
        try {
            frase = Validaciones.texto(vistaNueva.getFrase(), "La frase",
                    Validaciones.MAX_FRASE_IA);
        } catch (IllegalArgumentException e) {
            vistaNueva.mostrarAdvertencia(e.getMessage());
            return;
        }
        vistaNueva.habilitar(false);
        AtomicReference<DatosReservaSugerida> resultado = new AtomicReference<>();
        ejecutor.ejecutar(
                () -> resultado.set(servicio.extraerDatosReserva(frase)),
                () -> {
                    vistaNueva.habilitar(true);
                    DatosReservaSugerida datos = resultado.get();
                    vistaNueva.rellenar(datos);
                    if (!datos.categoriasNoEncontradas().isEmpty()) {
                        vistaNueva.mostrarAdvertencia("La IA mencionó categorías que no "
                                + "existen en el sistema: "
                                + String.join(", ", datos.categoriasNoEncontradas()) + ".");
                    }
                },
                error -> {
                    vistaNueva.habilitar(true);
                    vistaNueva.mostrarError(Errores.mensaje(error));
                });
    }

    void cancelar() {
        Reserva seleccionada = modelo.getSeleccionada();
        if (seleccionada == null) {
            vistaMisReservas.mostrarAdvertencia("Seleccione una reserva de la tabla.");
            return;
        }
        if (!vistaMisReservas.confirmarCancelacion(seleccionada.getId())) {
            return;
        }
        vistaMisReservas.habilitar(false);
        ejecutor.ejecutar(
                () -> servicio.cancelarReserva(seleccionada.getId(), funcionarioId),
                () -> {
                    vistaMisReservas.habilitar(true);
                    vistaMisReservas.mostrarInformacion("La reserva "
                            + seleccionada.getId() + " fue cancelada.");
                    modelo.limpiarSeleccion();
                    recargarReservas();
                },
                error -> {
                    vistaMisReservas.habilitar(true);
                    vistaMisReservas.mostrarError(Errores.mensaje(error));
                });
    }

    private void seleccionar(int fila) {
        List<Reserva> reservas = modelo.getReservas();
        if (fila < 0 || fila >= reservas.size()) {
            return;
        }
        modelo.seleccionar(reservas.get(fila));
    }

    private void imprimir() {
        List<Reserva> reservas = modelo.getReservas();
        if (reservas.isEmpty()) {
            vistaMisReservas.mostrarAdvertencia("No hay reservas para exportar.");
            return;
        }
        List<String[]> filas = new ArrayList<>();
        for (Reserva reserva : reservas) {
            filas.add(new String[]{reserva.getId(), reserva.getActividad(),
                    reserva.getFecha().toString(),
                    reserva.getHoraInicio() + " - " + reserva.getHoraFin(),
                    recursosDe(reserva), reserva.getEstado().name()});
        }
        Path destino = vistaMisReservas.elegirDestinoPdf("reservas.pdf");
        if (destino == null) {
            return;
        }
        vistaMisReservas.habilitar(false);
        ejecutor.ejecutar(
                () -> Reportes.generar("Reservas de " + funcionarioId, ENCABEZADOS,
                        filas, destino),
                () -> {
                    vistaMisReservas.habilitar(true);
                    vistaMisReservas.mostrarInformacion(
                            "Reporte generado en " + destino + ".");
                },
                error -> {
                    vistaMisReservas.habilitar(true);
                    vistaMisReservas.mostrarError(Errores.mensaje(error));
                });
    }

    private static String recursosDe(Reserva reserva) {
        return reserva.getRecursos().stream()
                .map(Recurso::getDescripcion)
                .reduce((primero, segundo) -> primero + ", " + segundo)
                .orElse("");
    }

    /** Categorías disponibles y reservas visibles. */
    private record Carga(List<CategoriaRecurso> categorias, List<Reserva> reservas) {
    }
}
