package com.clinica.historia.eventos;

/**
 * Contratos de eventos que Historia Clínica publica o consume (ver docs/04-contratos-eventos.md).
 * paciente.registrado NO incluye alergias ni ningún otro dato clínico.
 */
public final class Eventos {

    private Eventos() {}

    /** Publicado con routing key paciente.registrado. */
    public record PacienteRegistrado(String patientId, String documentNumber, String fullName,
                                     String email, String phone, String eps, String occurredAt) {}

    /** Consumido desde Agenda (cita.agendada / cita.cancelada). Solo los campos que se usan aquí. */
    public record CitaEvento(String appointmentId, String patientId, String doctorName, String specialty,
                             String date, String status, String notes) {}
}
