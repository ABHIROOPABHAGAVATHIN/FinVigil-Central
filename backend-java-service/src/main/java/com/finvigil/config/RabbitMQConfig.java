package com.finvigil.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "finvigil.exchange";
    public static final String DLX_NAME = "finvigil.dlx";
    public static final String DLQ_NAME = "finvigil.dlq";

    public static final String CREDIT_UNDERWRITING_QUEUE = "credit.underwriting.queue";
    public static final String CREDIT_DECISION_QUEUE = "credit.decision.queue";
    public static final String AML_TRANSACTION_QUEUE = "aml.transaction.queue";
    public static final String AML_ALERT_QUEUE = "aml.alert.queue";

    public static final String CREDIT_UNDERWRITING_ROUTING_KEY = "credit.underwriting";
    public static final String CREDIT_DECISION_ROUTING_KEY = "credit.decision";
    public static final String AML_TRANSACTION_ROUTING_KEY = "aml.transaction";
    public static final String AML_ALERT_ROUTING_KEY = "aml.alert";

    @Bean
    public DirectExchange finvigilExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("#");
    }

    private Map<String, Object> queueArguments() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_NAME);
        args.put("x-dead-letter-routing-key", "dlq.dead.letter");
        return args;
    }

    @Bean
    public Queue creditUnderwritingQueue() {
        return new Queue(CREDIT_UNDERWRITING_QUEUE, true, false, false, queueArguments());
    }

    @Bean
    public Queue creditDecisionQueue() {
        return new Queue(CREDIT_DECISION_QUEUE, true, false, false, queueArguments());
    }

    @Bean
    public Queue amlTransactionQueue() {
        return new Queue(AML_TRANSACTION_QUEUE, true, false, false, queueArguments());
    }

    @Bean
    public Queue amlAlertQueue() {
        return new Queue(AML_ALERT_QUEUE, true, false, false, queueArguments());
    }

    @Bean
    public Binding creditUnderwritingBinding() {
        return BindingBuilder.bind(creditUnderwritingQueue())
                .to(finvigilExchange())
                .with(CREDIT_UNDERWRITING_ROUTING_KEY);
    }

    @Bean
    public Binding creditDecisionBinding() {
        return BindingBuilder.bind(creditDecisionQueue())
                .to(finvigilExchange())
                .with(CREDIT_DECISION_ROUTING_KEY);
    }

    @Bean
    public Binding amlTransactionBinding() {
        return BindingBuilder.bind(amlTransactionQueue())
                .to(finvigilExchange())
                .with(AML_TRANSACTION_ROUTING_KEY);
    }

    @Bean
    public Binding amlAlertBinding() {
        return BindingBuilder.bind(amlAlertQueue())
                .to(finvigilExchange())
                .with(AML_ALERT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
