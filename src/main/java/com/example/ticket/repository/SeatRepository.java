package com.example.ticket.repository;

import com.example.ticket.entity.Seat;
import com.example.ticket.enums.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface SeatRepository
        extends JpaRepository<Seat, Long> {

    List<Seat> findByConcertId(Long concertId);
    boolean existsByConcertId(Long concertId);

    List<Seat> findByStatus(SeatStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       select s
       from Seat s
       where s.id in :ids
       """)
    List<Seat> findAllByIdForUpdate(List<Long> ids);

    @Modifying
    @Query("""
    UPDATE Seat s 
    SET s.status = :availableStatus,
        s.holdByUserId = NULL, 
        s.holdExpiresAt = NULL
    WHERE s.status = :holdStatus 
      AND s.holdExpiresAt < :now
""")
    int cleanupExpiredHolds(
            @Param("now") LocalDateTime now,
            @Param("holdStatus") SeatStatus holdStatus,
            @Param("availableStatus") SeatStatus availableStatus
    );

}
