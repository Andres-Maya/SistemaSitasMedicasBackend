package com.clinica.agenda.eventos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de mensajería (ADR-002). Todos los servicios declaran el mismo exchange
 * de tipo topic; cada consumidor declara SU cola y sus bindings.
 */
@Configuration
public class EventosConfig {

    public static final String EXCHANGE = "clinica.eventos";
    public static final String COLA_PACIENTES = "agenda.paciente-registrado";

    @Bean
    public TopicExchange clinicaExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue colaPacientes() {
        return QueueBuilder.durable(COLA_PACIENTES).build();
    }

    @Bean
    public Binding bindingPacientes(Queue colaPacientes, TopicExchange clinicaExchange) {
        return BindingBuilder.bind(colaPacientes).to(clinicaExchange).with("paciente.registrado");
    }

    // JSON sin acoplarse a las clases del publicador: el consumidor usa su propio tipo (lector tolerante)
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
