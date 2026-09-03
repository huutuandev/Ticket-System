package com.example.ticket.service;


import com.example.ticket.enums.SeatStatus;
import com.example.ticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class HoldCleanupJob {

    private final SeatRepository seatRepo;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void cleanExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();

        int updatedCount = seatRepo.cleanupExpiredHolds(
                now,
                SeatStatus.HOLD,
                SeatStatus.AVAILABLE
        );

        if (updatedCount > 0) {
            log.info("✅ Cleaned up {} expired seat holds at {}", updatedCount, now);
        } else {
            log.debug("Không có seat hold nào hết hạn cần dọn dẹp lúc {}", now);
        }
    }
}