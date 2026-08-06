package com.investwise.investment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology, shared verbatim by both services.
 * <p>
 * Simplified from three exchanges and six queues to one topic exchange, two
 * queues and a dead-letter queue. Routing keys still express intent
 * ({@code user.registered}, {@code subscription.changed}), so adding a consumer
 * needs no change here.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "investwise.events";
    public static final String DLX = "investwise.dlx";

    /** Consumed by the Investment Service. */
    public static final String USER_QUEUE = "investwise.user.events";
    /** Consumed by this service. */
    public static final String SUBSCRIPTION_QUEUE = "investwise.subscription.events";
    public static final String NOTIFY_QUEUE = "investwise.notifications";
    public static final String DEAD_LETTER_QUEUE = "investwise.dead-letter";

    public static final String RK_USER_REGISTERED = "user.registered";
    public static final String RK_USER_STATUS = "user.status";
    public static final String RK_SUBSCRIPTION_CHANGED = "subscription.changed";
    public static final String RK_NOTIFY = "notify";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Queue userQueue() {
        return durable(USER_QUEUE);
    }

    /**
     * Two queues rather than one, so each listener method has an unambiguous
     * payload type. Sharing a queue between differently-shaped messages would need
     * type headers the producer cannot supply across service boundaries.
     */
    @Bean
    public Queue subscriptionQueue() {
        return durable(SUBSCRIPTION_QUEUE);
    }

    @Bean
    public Queue notifyQueue() {
        return durable(NOTIFY_QUEUE);
    }

    /** A message that cannot be processed must be inspectable, not silently dropped. */
    private Queue durable(String name) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", "dead")
                .build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("dead");
    }

    /** Everything a user does routes to the queue the Investment Service drains. */
    @Bean
    public Binding userEventsBinding() {
        return BindingBuilder.bind(userQueue()).to(eventsExchange()).with("user.#");
    }

    /** Subscription changes and notification requests come back to this service. */
    @Bean
    public Binding subscriptionBinding() {
        return BindingBuilder.bind(subscriptionQueue()).to(eventsExchange()).with("subscription.#");
    }

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(notifyQueue()).to(eventsExchange()).with("notify");
    }

    /**
     * The producer stamps its own class name into the {@code __TypeId__} header,
     * and that class does not exist in the consuming service. Telling the mapper to
     * infer the type from the listener method signature instead is what makes the
     * two services able to exchange records at all.
     */
    @Bean
    public MessageConverter jsonConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("com.investwise.*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(converter);
        return template;
    }
}
