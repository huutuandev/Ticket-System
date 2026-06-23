package com.example.ticket.repository;

import com.example.ticket.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SeatRepository
        extends JpaRepository<Seat, Long> {

    List<Seat> findByConcertId(Long concertId);
    boolean existsByConcertId(Long concertId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       select s
       from Seat s
       where s.id in :ids
       """)
    List<Seat> findAllByIdForUpdate(List<Long> ids);

}
