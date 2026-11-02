package com.clinica.agenda.eventos;

/**
 * Contratos de eventos que Agenda publica o consume (ver docs/04-contratos-eventos.md).
 * Los eventos no transportan datos clínicos: solo identificadores y datos de la cita.
 */
public final class Eventos {

    private Eventos() {}

    /** Publicado con routing key cita.agendada o cita.cancelada. */
    public record CitaEvento(String appointmentId, String patientId, String doctorId, String doctorName,
                             String specialty, String date, String status, String notes,
                             String occurredAt) {}

    /** Consumido desde Historia Clínica (routing key paciente.registrado). */
    public record PacienteRegistrado(String patientId, String documentNumber, String fullName, String eps) {}
}
