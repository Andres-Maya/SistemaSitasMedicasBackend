package com.clinica.agenda.eventos;

import com.clinica.agenda.dominio.Appointment;
import com.clinica.agenda.dominio.EventosAgenda;
import com.clinica.agenda.eventos.Eventos.CitaEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PublicadorRabbit implements EventosAgenda {

    private static final Logger log = LoggerFactory.getLogger(PublicadorRabbit.class);

    private final RabbitTemplate rabbitTemplate;

    public PublicadorRabbit(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void citaAgendada(Appointment cita) {
        publicar("cita.agendada", cita);
    }

    @Override
    public void citaCancelada(Appointment cita) {
        publicar("cita.cancelada", cita);
    }

    private void publicar(String routingKey, Appointment cita) {
        CitaEvento evento = new CitaEvento(cita.getId(), cita.getPatientId(), cita.getDoctorId(),
                cita.getDoctorName(), cita.getSpecialty(), cita.getDate(), cita.getStatus(),
                cita.getNotes(), Instant.now().toString());
        try {
            rabbitTemplate.convertAndSend(EventosConfig.EXCHANGE, routingKey, evento);
            log.info("Evento publicado: {} {}", routingKey, cita.getId());
        } catch (AmqpException e) {
            // Riesgo R1 (docs/06-riesgos.md): sin outbox, el estado queda guardado pero el evento se pierde
            log.error("No se pudo publicar {} para {}: {}", routingKey, cita.getId(), e.getMessage());
        }
    }
}
