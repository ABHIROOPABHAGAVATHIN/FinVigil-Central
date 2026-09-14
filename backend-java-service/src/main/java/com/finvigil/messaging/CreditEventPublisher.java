package com.finvigil.messaging;

import com.finvigil.config.RabbitMQConfig;
import com.finvigil.messaging.dto.CreditUnderwritingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
public class CreditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CreditEventPublisher.class);
    private final AmqpTemplate amqpTemplate;

    public CreditEventPublisher(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    public void publishCreditUnderwritingEvent(CreditUnderwritingEvent event) {
        try {
            log.info("Publishing credit underwriting event to RabbitMQ: appUuid={}, custUuid={}, loanAmount={}, score={}",
                    event.getApplicationUuid(), event.getCustomerUuid(), event.getLoanAmount(), event.getCreditScore());

            amqpTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.CREDIT_UNDERWRITING_ROUTING_KEY,
                    event
            );

            log.info("Successfully published credit underwriting event: appUuid={}", event.getApplicationUuid());
        } catch (Exception e) {
            log.warn("RabbitMQ publish deferred/offline: appUuid={}, error={}",
                    event.getApplicationUuid(), e.getMessage());
        }
    }
}
