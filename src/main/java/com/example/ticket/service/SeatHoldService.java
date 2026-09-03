package com.example.ticket.service;

import com.example.ticket.entity.Seat;
import com.example.ticket.enums.SeatStatus;
import com.example.ticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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
        Boolean deleted = redisTemplate.delete(holdId);
        log.debug("Release hold (Redis delete). Key: {}, Deleted: {}", holdId, deleted);

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
                log.warn("Invalid holdId format khi releaseHold: {}", holdId);
                // Ignore invalid key format
            }
        }
    }

    @Transactional
    public void releaseHolds(List<Long> seatIds) {
        if (seatIds == null) return;
        for (Long seatId : seatIds) {
            String holdId = SEAT_HOLD_KEY_PREFIX + seatId;
            Boolean deleted = redisTemplate.delete(holdId);
            log.debug("Release hold (Redis delete). Key: {}, Deleted: {}", holdId, deleted);
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
