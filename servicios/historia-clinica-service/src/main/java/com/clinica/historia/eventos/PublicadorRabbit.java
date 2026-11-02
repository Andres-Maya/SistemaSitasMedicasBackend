package com.clinica.historia.eventos;

import com.clinica.historia.dominio.EventosHistoria;
import com.clinica.historia.dominio.Patient;
import com.clinica.historia.eventos.Eventos.PacienteRegistrado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PublicadorRabbit implements EventosHistoria {

    private static final Logger log = LoggerFactory.getLogger(PublicadorRabbit.class);

    private final RabbitTemplate rabbitTemplate;

    public PublicadorRabbit(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void pacienteRegistrado(Patient p) {
        PacienteRegistrado evento = new PacienteRegistrado(p.getId(), p.getDocumentNumber(), p.getFullName(),
                p.getEmail(), p.getPhone(), p.getEps(), Instant.now().toString());
        try {
            rabbitTemplate.convertAndSend(EventosConfig.EXCHANGE, "paciente.registrado", evento);
            log.info("Evento publicado: paciente.registrado {}", p.getId());
        } catch (AmqpException e) {
            // Riesgo R1 (docs/06-riesgos.md): sin outbox el paciente existe pero Agenda no se entera
            log.error("No se pudo publicar paciente.registrado para {}: {}", p.getId(), e.getMessage());
        }
    }
}
