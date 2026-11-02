package com.clinica.agenda.dominio;

/**
 * Puerto de salida para los hechos de negocio que Agenda anuncia al resto del sistema.
 * La implementación (RabbitMQ) vive en el paquete eventos.
 */
public interface EventosAgenda {
    void citaAgendada(Appointment cita);
    void citaCancelada(Appointment cita);
}
