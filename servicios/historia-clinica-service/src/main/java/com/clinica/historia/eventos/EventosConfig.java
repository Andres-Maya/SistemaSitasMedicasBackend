package com.clinica.historia.eventos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventosConfig {

    public static final String EXCHANGE = "clinica.eventos";
    public static final String COLA_CITAS = "historia.citas";

    @Bean
    public TopicExchange clinicaExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue colaCitas() {
        return QueueBuilder.durable(COLA_CITAS).build();
    }

    // cita.agendada y cita.cancelada llegan a la misma cola
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
