package com.finvigil.messaging;

import com.finvigil.aml.service.AmlAlertService;
import com.finvigil.config.RabbitMQConfig;
import com.finvigil.messaging.dto.AmlAlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AmlEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AmlEventConsumer.class);

    private final AmlAlertService amlAlertService;

    public AmlEventConsumer(AmlAlertService amlAlertService) {
        this.amlAlertService = amlAlertService;
    }

    @RabbitListener(queues = RabbitMQConfig.AML_ALERT_QUEUE)
    public void consumeAmlAlert(AmlAlertEvent event) {
        log.info("Received AML Alert Event: alertUuid={}, txnUuid={}, custUuid={}, riskLevel={}, hybridScore={}",
                event.getAlertUuid(), event.getTransactionUuid(), event.getCustomerUuid(),
                event.getRiskLevel(), event.getHybridScore());

        try {
            amlAlertService.processAlertEvent(event);
            log.info("Successfully ingested AML Alert [{}] for customer [{}]",
                    event.getAlertUuid(), event.getCustomerUuid());
        } catch (Exception ex) {
            log.error("Failed to process incoming AML Alert [{}] for customer [{}]:",
                    event.getAlertUuid(), event.getCustomerUuid(), ex);
            throw ex; // Let RabbitMQ handle retry / dead-letter
        }
    }
}
