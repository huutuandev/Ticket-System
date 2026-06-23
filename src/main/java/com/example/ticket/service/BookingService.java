package com.example.ticket.service;


import com.example.ticket.dto.request.CreateBookingRequest;
import com.example.ticket.dto.response.BookingResponse;
import com.example.ticket.entity.Booking;
import com.example.ticket.entity.BookingSeat;
import com.example.ticket.entity.Seat;
import com.example.ticket.entity.User;
import com.example.ticket.enums.BookingStatus;
import com.example.ticket.enums.SeatStatus;
import com.example.ticket.repository.BookingRepository;
import com.example.ticket.repository.BookingSeatRepository;
import com.example.ticket.repository.SeatRepository;
import com.example.ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final BookingSeatRepository bookingSeatRepo;
    private final SeatRepository seatRepo;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest bookingRequest){

        User user =  userRepo.findById(bookingRequest.getUserId())
                .orElseThrow(()-> new UsernameNotFoundException("Không tìm thấy user"));


        List<Seat> seats =
                seatRepo.findAllById(bookingRequest.getSeatIds());


        for (Seat seat : seats) {

            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Seat unavailable"
                );
            }

            seat.setStatus(SeatStatus.BOOKED);
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStatus(BookingStatus.PAID);
        booking.setBookingDate(LocalDateTime.now());
        BigDecimal totalAmount =
                seats.stream()
                        .map(Seat::getPrice)
                        .reduce(BigDecimal.ZERO,
                                BigDecimal::add);

        booking.setTotalAmount(totalAmount);
        bookingRepo.save(booking);

        for (Seat seat : seats) {

            BookingSeat bookingSeat =
                    new BookingSeat();

            bookingSeat.setBooking(booking);

            bookingSeat.setSeat(seat);

            bookingSeat.setPrice(seat.getPrice());

            bookingSeatRepo.save(bookingSeat);

            seat.setStatus(SeatStatus.BOOKED);
        }

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus().name())
                .build();
    }
}
