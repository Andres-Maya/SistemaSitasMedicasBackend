package com.clinica.agenda.eventos;

import com.clinica.agenda.dominio.AgendaService;
import com.clinica.agenda.dominio.PacienteConocido;
import com.clinica.agenda.eventos.Eventos.PacienteRegistrado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorPacientes {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorPacientes.class);

    private final AgendaService agendaService;

    public ConsumidorPacientes(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    // Idempotente: recibir dos veces el mismo evento deja la misma proyección
    @RabbitListener(queues = EventosConfig.COLA_PACIENTES)
    public void alRegistrarPaciente(PacienteRegistrado evento) {
        agendaService.registrarPacienteConocido(new PacienteConocido(
                evento.patientId(), evento.documentNumber(), evento.fullName(), evento.eps()));
        log.info("Paciente {} disponible para agendar (EPS {})", evento.patientId(), evento.eps());
    }
}
