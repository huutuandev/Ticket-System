package com.example.ticket.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final LettuceBasedProxyManager<String> proxyManager;

    public Bucket resolveBucket(String key, String role) {
        return proxyManager.builder().build(key, () -> getConfig(role));
    }

    private BucketConfiguration getConfig(String role) {
        long capacity;
        Duration period;

        if (role == null) {
            capacity = 5;
            period = Duration.ofMinutes(1); // ANONYMOUS: 5 req/phút
        } else {
            switch (role.toUpperCase()) {
                case "ROLE_ADMIN" -> { capacity = 10000; period = Duration.ofSeconds(1); }
                case "ROLE_VIP"  -> { capacity = 50;    period = Duration.ofMinutes(1); }
                default          -> { capacity = 10;    period = Duration.ofMinutes(1); } // USER: 10 req/phút
            }
        }

        // intervally: refill toàn bộ SAU KHI hết period, không refill dần dần
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, period));
        return BucketConfiguration.builder().addLimit(limit).build();
    }
}
