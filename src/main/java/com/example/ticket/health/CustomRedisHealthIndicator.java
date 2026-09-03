package com.example.ticket.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component("customRedisHealthIndicator")
@RequiredArgsConstructor
public class CustomRedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            String pingResponse = connection.ping();
            connection.close();
            if ("PONG".equalsIgnoreCase(pingResponse)) {
                return Health.up().withDetail("status", "Redis is responding to PING").build();
            }
            return Health.down().withDetail("error", "Unexpected ping response: " + pingResponse).build();
        } catch (Exception e) {
            return Health.down(e).withDetail("error", "Cannot connect to Redis").build();
        }
    }
}
