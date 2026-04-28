package com.vestify.backend.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String VTON_QUEUE = "vton_task_queue";

    @Bean
    public Queue vtonQueue() {
        return new Queue(VTON_QUEUE, true);
    }

    // IDE'nin  uyarısını susturmak için bu satırı ekledik
    @SuppressWarnings("removal")
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}