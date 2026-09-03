package com.example.ticket.service.util;

import com.example.ticket.enums.SeatStatus;
import com.example.ticket.repository.SeatRepository;
import com.example.ticket.service.HoldCleanupJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HoldCleanupJobTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private HoldCleanupJob holdCleanupJob;

    // Kiểm tra logic cleanupExpiredHolds gọi đúng repository method và log ra
    @Test
    void cleanExpiredHolds_success() {
        // Arrange
        when(seatRepository.cleanupExpiredHolds(any(LocalDateTime.class), eq(SeatStatus.HOLD), eq(SeatStatus.AVAILABLE)))
                .thenReturn(5);

        // Act
        holdCleanupJob.cleanExpiredHolds();

        // Assert
        verify(seatRepository, times(1)).cleanupExpiredHolds(any(LocalDateTime.class), eq(SeatStatus.HOLD), eq(SeatStatus.AVAILABLE));
    }
    
    // Kiểm tra logic cleanupExpiredHolds khi không có record nào bị update
    @Test
    void cleanExpiredHolds_noUpdated() {
        // Arrange
        when(seatRepository.cleanupExpiredHolds(any(LocalDateTime.class), eq(SeatStatus.HOLD), eq(SeatStatus.AVAILABLE)))
                .thenReturn(0);

        // Act
        holdCleanupJob.cleanExpiredHolds();

        // Assert
        verify(seatRepository, times(1)).cleanupExpiredHolds(any(LocalDateTime.class), eq(SeatStatus.HOLD), eq(SeatStatus.AVAILABLE));
    }
}
