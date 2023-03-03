package br.com.egc.api.spring.rabbitmq.retry.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
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

    @Value("${rabbitmq.version.greater.than.or.equal.to.3.10}")
    private Boolean versionGreaterThanOrEqualTo_3_10;

    public static final String UNDELIVERED_QUEUE = "api.spring.rabbitmq.retry.main.queue.undelivered";

    private String RETRY_QUEUE = "api.spring.rabbitmq.retry.main.queue.retry";

    public static final String QUEUE = "api.spring.rabbitmq.retry.main.queue";

    private Long count;

    @RabbitListener(queues = QUEUE)
    public void primary(Message message, @Header(required = false, name = "x-death") Map<String, ?> xDeath) throws Exception {
        log.info("version greater than or equal to 3.10 is " + versionGreaterThanOrEqualTo_3_10);

        log.info("Message read from Queue: " + message);

        if (versionGreaterThanOrEqualTo_3_10) {
            if (checkRetryCount(xDeath, message)) {
                sendToUndelivered(message);
                return;
            }
            throw new Exception("Random error");

        } else {
            if (checkRetryCount(xDeath, message)) {
                sendToUndelivered(message);
                return;
            }
            this.rabbitTemplate.convertAndSend(RETRY_QUEUE, message);
        }

    }

    private boolean checkRetryCount(Map<String, ?> xDeath, Message message) {
        if (xDeath != null && !xDeath.isEmpty()) {
            Long count = (Long) xDeath.get("count");
            if (!versionGreaterThanOrEqualTo_3_10) {
                Long redelivery = count;
                message.getMessageProperties().setHeader("x-death.count", redelivery++);
            }
            return count >= retryCount;
        }
        return false;
    }

    private void sendToUndelivered(Message jsonMesssage) {
        log.warn("maximum retry reached, send message to the undelivered queue, msg: {}", jsonMesssage);
        this.rabbitTemplate.convertAndSend(UNDELIVERED_QUEUE, jsonMesssage);
    }
}
