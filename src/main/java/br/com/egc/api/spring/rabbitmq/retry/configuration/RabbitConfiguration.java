package br.com.egc.api.spring.rabbitmq.retry.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {
    private String EXCHANGE_NAME = "api.spring.rabbitmq.retry.retry.exchange";
    public String QUEUE = "api.spring.rabbitmq.retry.main.queue";
    private  String RETRY_QUEUE = "api.spring.rabbitmq.retry.main.queue.retry";
    public   String UNDELIVERED_QUEUE = "api.spring.rabbitmq.retry.main.queue.undelivered";
    private   String MAIN_ROUTING_KEY = "api.spring.rabbitmq.retry.main.routing.key";
    @Value("${rabbitmq.retry.delay-in-ms}")
    private Integer retryDelay;

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    Queue mainQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(EXCHANGE_NAME)
                .deadLetterRoutingKey(RETRY_QUEUE)
                .quorum()
                .build();
    }

    @Bean
    Queue retryQueue() {
        return QueueBuilder.durable(RETRY_QUEUE)
                .deadLetterExchange(EXCHANGE_NAME)
                .deadLetterRoutingKey(MAIN_ROUTING_KEY)
                .ttl(retryDelay)
                .quorum()
                .build();
    }

    @Bean
    Queue undeliveredQueue() {
        return QueueBuilder.durable(UNDELIVERED_QUEUE)
                .quorum()
                .build();
    }

    @Bean
    Binding mainBinding(Queue mainQueue, TopicExchange exchange) {
        return BindingBuilder.bind(mainQueue).to(exchange).with(MAIN_ROUTING_KEY);
    }

    @Bean
    Binding retryBinding(Queue retryQueue, TopicExchange exchange) {
        return BindingBuilder.bind(retryQueue).to(exchange).with(RETRY_QUEUE);
    }

    @Bean
    Binding undeliveredBinding(Queue undeliveredQueue, TopicExchange exchange) {
        return BindingBuilder.bind(undeliveredQueue).to(exchange).with(UNDELIVERED_QUEUE);
    }
}
