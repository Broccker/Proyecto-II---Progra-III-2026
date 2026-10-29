package com.sistema.common.modelo;

import com.sistema.common.excepciones.ValidacionException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Reserva de uno o varios recursos para una actividad.
 *
 * <p>Es inmutable salvo el estado, que cambia al cancelar la reserva.</p>
 */
public class Reserva implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final Funcionario funcionario;
    private final String actividad;
    private final LocalDate fecha;
    private final LocalTime horaInicio;
    private final LocalTime horaFin;
    private final List<Recurso> recursos;
    private EstadoReserva estado;

    public Reserva(String id, Funcionario funcionario, String actividad, LocalDate fecha,
                   LocalTime horaInicio, LocalTime horaFin, List<Recurso> recursos,
                   EstadoReserva estado) {
        if (id == null || id.isBlank()) {
            throw new ValidacionException("El identificador de la reserva no puede estar vacío.");
        }
        if (funcionario == null) {
            throw new ValidacionException("La reserva debe tener un funcionario asignado.");
        }
        if (actividad == null || actividad.isBlank()) {
            throw new ValidacionException("La actividad de la reserva no puede estar vacía.");
        }
        if (fecha == null) {
            throw new ValidacionException("La fecha de la reserva no puede ser nula.");
        }
        if (horaInicio == null || horaFin == null) {
            throw new ValidacionException("Debe indicar la hora de inicio y de fin.");
        }
        if (!horaFin.isAfter(horaInicio)) {
            throw new ValidacionException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        if (recursos == null || recursos.isEmpty()) {
            throw new ValidacionException("La reserva debe incluir al menos un recurso.");
        }
        setEstado(estado);

        this.id = id.trim();
        this.funcionario = funcionario;
        this.actividad = actividad.trim();
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.recursos = List.copyOf(recursos);
    }

    public String getId() {
        return id;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public String getActividad() {
        return actividad;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public List<Recurso> getRecursos() {
        return recursos;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        if (estado == null) {
            throw new ValidacionException("El estado de la reserva no puede ser nulo.");
        }
        this.estado = estado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reserva otra)) {
            return false;
        }
        return Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
