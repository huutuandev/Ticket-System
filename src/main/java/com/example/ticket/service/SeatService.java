package com.example.ticket.service;


import com.example.ticket.dto.request.GenerateSeatsRequest;
import com.example.ticket.dto.response.SeatResponse;
import com.example.ticket.entity.Concert;
import com.example.ticket.entity.Seat;
import com.example.ticket.enums.SeatStatus;
import com.example.ticket.enums.SeatType;
import com.example.ticket.repository.ConcertRepository;
import com.example.ticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {
    private final ConcertRepository concertRepo;
    private final SeatRepository seatRepo;


    @Transactional
    public List<SeatResponse> generateSeats(GenerateSeatsRequest request, Long concertId){

        if(request.getRows() > 26){
            throw new RuntimeException("Maximum rows is 26");
        }

        Concert concert = concertRepo.findById(concertId)
                .orElseThrow(()-> new RuntimeException("Không tìm thấy concert này " + concertId));

        if (seatRepo.existsByConcertId(concert.getId())) {
            throw new RuntimeException("Concert đã được tạo ghế");
        }


        List<Seat> seats = new ArrayList<>();

        for (int i = 0; i < request.getRows(); i++) {

            char rowName = (char) ('A' + i);

            for (int j = 1; j <= request.getSeatsPerRow(); j++) {

                Seat seat = new Seat();

                seat.setRowName(String.valueOf(rowName));
                seat.setSeatNumber((long) j);

                seat.setPrice(BigDecimal.valueOf(100000));

                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setType(SeatType.STANDARD);

                seat.setConcert(concert);

                seats.add(seat);
            }
        }

        List<Seat> savedSeats = seatRepo.saveAll(seats);

        return savedSeats.stream()
                .map(this::toResponse)
                .toList();

    }

    public List<SeatResponse> getSeatsByConcert(Long concertId){
        List<Seat> seats = seatRepo.findByConcertId(concertId);
        return seats.stream()
                .map(this::toResponse)
                .toList();
    }

    private SeatResponse toResponse(Seat seat){
        SeatResponse response = new SeatResponse();
        response.setId(seat.getId());
        response.setRowName(seat.getRowName());
        response.setSeatNumber(seat.getSeatNumber());
        response.setType(seat.getType().name());
        response.setPrice(seat.getPrice());
        response.setStatus(seat.getStatus().name());
        return response;
    }
}
