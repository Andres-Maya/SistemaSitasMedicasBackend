package com.clinica.historia.eventos;

import com.clinica.historia.dominio.AppointmentView;
import com.clinica.historia.dominio.HistoriaClinicaService;
import com.clinica.historia.eventos.Eventos.CitaEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorCitas {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorCitas.class);

    private final HistoriaClinicaService historiaClinicaService;

    public ConsumidorCitas(HistoriaClinicaService historiaClinicaService) {
        this.historiaClinicaService = historiaClinicaService;
    }

    @RabbitListener(queues = EventosConfig.COLA_CITAS)
    public void alCambiarCita(CitaEvento evento, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        historiaClinicaService.actualizarCita(new AppointmentView(evento.appointmentId(), evento.patientId(),
                evento.doctorName(), evento.specialty(), evento.date(), evento.status(), evento.notes()));
        log.info("[{}] cita {} reflejada en la historia del paciente {}",
                routingKey, evento.appointmentId(), evento.patientId());
    }
}
