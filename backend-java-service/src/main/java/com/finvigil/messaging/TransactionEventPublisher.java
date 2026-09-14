package com.finvigil.messaging;

import com.finvigil.config.RabbitMQConfig;
import com.finvigil.messaging.dto.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);

    private final AmqpTemplate amqpTemplate;

    public TransactionEventPublisher(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    public void publishTransactionEvent(TransactionEvent event) {
        try {
            log.info("Publishing real-time transaction event for AML monitoring: txnUuid={}, custUuid={}, amount={}, velocityCount={}",
                    event.getTransactionUuid(), event.getCustomerUuid(), event.getAmount(), event.getVelocityCount1m());

            amqpTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.AML_TRANSACTION_ROUTING_KEY,
                    event
            );
        } catch (Exception e) {
            log.warn("Failed to publish transaction event to RabbitMQ (broker unavailable?): txnUuid={}, error={}",
                    event.getTransactionUuid(), e.getMessage());
        }
    }
}
