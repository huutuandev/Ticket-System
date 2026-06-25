package com.example.ticket.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ===== Constants =====
    public static final String BOOKING_EXCHANGE   = "booking.exchange";
    public static final String BOOKING_QUEUE      = "booking.queue";
    public static final String PDF_QUEUE          = "pdf.queue";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String ROUTING_BOOKING_CREATED = "booking.created";

    // ===== DLQ Constants =====
    public static final String DLX_EXCHANGE = "dlx.exchange";
    public static final String BOOKING_DLQ  = "booking.dlq";

    // ===== DLX Exchange =====
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    // ===== DLQ =====
    @Bean
    public Queue bookingDlq() {
        return QueueBuilder.durable(BOOKING_DLQ).build();
    }

    @Bean
    public Binding dlqBinding(Queue bookingDlq, DirectExchange dlxExchange) {
        return BindingBuilder
                .bind(bookingDlq)
                .to(dlxExchange)
                .with(BOOKING_DLQ);
    }

    // ===== Main Exchange =====
    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    // ===== Queues (với DLX args) =====
    @Bean
    public Queue bookingQueue() {
        return QueueBuilder.durable(BOOKING_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", BOOKING_DLQ)
                .build();
    }

    @Bean
    public Queue pdfQueue() {
        return QueueBuilder.durable(PDF_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", BOOKING_DLQ)
                .build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", BOOKING_DLQ)
                .build();
    }

    // ===== Bindings =====
    @Bean
    public Binding bookingBinding(Queue bookingQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingQueue).to(bookingExchange).with(ROUTING_BOOKING_CREATED);
    }

    @Bean
    public Binding pdfBinding(Queue pdfQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(pdfQueue).to(bookingExchange).with(ROUTING_BOOKING_CREATED);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(notificationQueue).to(bookingExchange).with(ROUTING_BOOKING_CREATED);
    }

    // ===== JSON Converter =====
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}