package com.example.ticket.service;

import com.example.ticket.entity.Seat;
import com.example.ticket.enums.SeatStatus;
import com.example.ticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SeatRepository seatRepository;
    private static final String SEAT_HOLD_KEY_PREFIX = "hold:seat:";

    public boolean isHeld(String holdId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(holdId));
    }

    public boolean isHoldValidForUser(Long seatId, Long userId) {
        String key = SEAT_HOLD_KEY_PREFIX + seatId;
        Object val = redisTemplate.opsForValue().get(key);
        return val != null && val.toString().equals(userId.toString());
    }

    @Transactional
    public void releaseHold(String holdId) {
        redisTemplate.delete(holdId);

        if (holdId != null && holdId.startsWith(SEAT_HOLD_KEY_PREFIX)) {
            try {
                Long seatId = Long.parseLong(holdId.substring(SEAT_HOLD_KEY_PREFIX.length()));
                seatRepository.findById(seatId).ifPresent(seat -> {
                    if (seat.getStatus() == SeatStatus.HOLD) {
                        seat.setStatus(SeatStatus.AVAILABLE);
                        seat.setHoldByUserId(null);
                        seat.setHoldExpiresAt(null);
                        seatRepository.save(seat);
                    }
                });
            } catch (NumberFormatException e) {
                // Ignore invalid key format
            }
        }
    }

    @Transactional
    public void releaseHolds(List<Long> seatIds) {
        if (seatIds == null) return;
        for (Long seatId : seatIds) {
            String holdId = SEAT_HOLD_KEY_PREFIX + seatId;
            redisTemplate.delete(holdId);
            seatRepository.findById(seatId).ifPresent(seat -> {
                if (seat.getStatus() == SeatStatus.HOLD) {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setHoldByUserId(null);
                    seat.setHoldExpiresAt(null);
                    seatRepository.save(seat);
                }
            });
        }
    }
}
