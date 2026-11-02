package com.clinica.notificaciones.eventos;

import com.clinica.notificaciones.dominio.NotificacionService;
import com.clinica.notificaciones.dominio.NotificacionService.Contacto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Notificaciones solo consume: no publica eventos ni expone escrituras a otros servicios. */
@Component
public class ConsumidorEventos {

    // Lector tolerante: cada record declara únicamente los campos que este servicio usa
    public record PacienteRegistrado(String patientId, String fullName, String email, String phone) {}

    public record CitaEvento(String appointmentId, String patientId, String doctorName, String specialty,
                             String date, String status) {}

    private final NotificacionService notificacionService;

    public ConsumidorEventos(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @RabbitListener(queues = EventosConfig.COLA_PACIENTES)
    public void alRegistrarPaciente(PacienteRegistrado evento) {
        notificacionService.registrarContacto(evento.patientId(),
                new Contacto(evento.fullName(), evento.email(), evento.phone()));
    }

    @RabbitListener(queues = EventosConfig.COLA_CITAS)
    public void alCambiarCita(CitaEvento evento) {
        notificacionService.avisarCambioCita(evento.appointmentId(), evento.patientId(), evento.status(),
                evento.specialty(), evento.doctorName(), evento.date());
    }
}
