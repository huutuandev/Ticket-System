package com.example.ticket.service;

import com.example.ticket.entity.Seat;
import com.example.ticket.enums.SeatStatus;
import com.example.ticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
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
            System.out.println("✅ Cleaned up " + updatedCount + " expired seat holds at " + now);
        }
    }
}