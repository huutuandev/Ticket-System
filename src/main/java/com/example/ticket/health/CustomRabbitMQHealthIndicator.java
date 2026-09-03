package com.example.ticket.health;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("customRabbitMQHealthIndicator")
@RequiredArgsConstructor
public class CustomRabbitMQHealthIndicator implements HealthIndicator {

    private final ConnectionFactory connectionFactory;

    @Override
    public Health health() {
        try {
            Connection connection = connectionFactory.createConnection();
            boolean isOpen = connection.isOpen();
            connection.close();
            
            if (isOpen) {
                return Health.up().withDetail("status", "RabbitMQ connection is open").build();
            }
            return Health.down().withDetail("error", "RabbitMQ connection is closed").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("error", "Cannot connect to RabbitMQ").build();
        }
    }
}
