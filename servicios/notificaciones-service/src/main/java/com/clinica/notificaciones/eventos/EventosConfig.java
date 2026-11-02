package com.clinica.notificaciones.eventos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventosConfig {

    public static final String EXCHANGE = "clinica.eventos";
    public static final String COLA_PACIENTES = "notificaciones.paciente-registrado";
    public static final String COLA_CITAS = "notificaciones.citas";

    @Bean
    public TopicExchange clinicaExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue colaPacientes() {
        return QueueBuilder.durable(COLA_PACIENTES).build();
    }

    @Bean
    public Queue colaCitas() {
        return QueueBuilder.durable(COLA_CITAS).build();
    }

    @Bean
    public Binding bindingPacientes(Queue colaPacientes, TopicExchange clinicaExchange) {
        return BindingBuilder.bind(colaPacientes).to(clinicaExchange).with("paciente.registrado");
    }

    @Bean
    public Binding bindingCitas(Queue colaCitas, TopicExchange clinicaExchange) {
        return BindingBuilder.bind(colaCitas).to(clinicaExchange).with("cita.*");
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
