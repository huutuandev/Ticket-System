package com.example.ticket.repository;

import com.example.ticket.entity.Booking;
import com.example.ticket.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"bookingSeats", "bookingSeats.seat", "bookingSeats.seat.concert"})
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId")
    Page<Booking> findByUserIdWithDetails(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"bookingSeats", "bookingSeats.seat", "bookingSeats.seat.concert"})
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    Page<Booking> findByUserIdAndStatusWithDetails(@Param("userId") Long userId, @Param("status") BookingStatus status, Pageable pageable);
}
