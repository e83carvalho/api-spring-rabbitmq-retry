package br.com.egc.api.spring.rabbitmq.retry.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class RetryingRabbitListener {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Value("${rabbitmq.retry.count}")
    private Integer retryCount;

    public static final String UNDELIVERED_QUEUE = "api.spring.rabbitmq.retry.main.queue.undelivered";

    public static final String QUEUE = "api.spring.rabbitmq.retry.main.queue";

    @RabbitListener(queues = QUEUE)
    public void primary(String jsonMesssage, @Header(required = false, name = "x-death") Map<String, ?> xDeath) throws Exception {
        log.info("Message read from Queue: " + jsonMesssage);
        if (checkRetryCount(xDeath)) {
            sendToUndelivered(jsonMesssage);
            return;
        }
        throw new Exception("Random error");
    }
    private boolean checkRetryCount(Map<String, ?> xDeath) {
        if (xDeath != null && !xDeath.isEmpty()) {
            Long count = (Long) xDeath.get("count");
            return count >= retryCount;
        }
        return false;
    }
    private void sendToUndelivered(String jsonMesssage) {
        log.warn("maximum retry reached, send message to the undelivered queue, msg: {}", jsonMesssage);
        this.rabbitTemplate.convertAndSend(UNDELIVERED_QUEUE, jsonMesssage);
    }
}
