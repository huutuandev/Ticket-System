package com.example.ticket.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ===== Booking flow =====
    public static final String BOOKING_EXCHANGE   = "booking.exchange";
    public static final String BOOKING_QUEUE      = "booking.queue";   // email worker cho booking
    public static final String PDF_QUEUE          = "pdf.queue";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String ROUTING_BOOKING_CREATED = "booking.created";

    // ===== OTP flow
    public static final String OTP_EXCHANGE = "otp.exchange";
    public static final String OTP_QUEUE    = "otp.email.queue";
    public static final String ROUTING_OTP_SENT = "otp.sent";

    // ===== DLX =====
    public static final String DLX_EXCHANGE = "dlx.exchange";
    public static final String BOOKING_DLQ  = "booking.dlq";

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue bookingDlq() {
        return QueueBuilder.durable(BOOKING_DLQ).build();
    }

    @Bean
    public Binding dlqBinding(Queue bookingDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(bookingDlq).to(dlxExchange).with(BOOKING_DLQ);
    }

    // ===== Booking Exchange + Queues =====
    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

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

    // ===== OTP Exchange + Queue (độc lập, chỉ 1 consumer) =====
    @Bean
    public DirectExchange otpExchange() {
        return new DirectExchange(OTP_EXCHANGE);
    }

    @Bean
    public Queue otpQueue() {
        return QueueBuilder.durable(OTP_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", BOOKING_DLQ)
                .build();
    }

    @Bean
    public Binding otpBinding(Queue otpQueue, DirectExchange otpExchange) {
        return BindingBuilder.bind(otpQueue).to(otpExchange).with(ROUTING_OTP_SENT);
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