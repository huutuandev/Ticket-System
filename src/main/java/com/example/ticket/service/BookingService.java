package com.example.ticket.service;


import com.example.ticket.dto.request.ConfirmBookingRequest;
import com.example.ticket.dto.request.CreateBookingRequest;
import com.example.ticket.dto.response.BookingResponse;
import com.example.ticket.entity.Booking;
import com.example.ticket.entity.BookingSeat;
import com.example.ticket.entity.Seat;
import com.example.ticket.entity.User;
import com.example.ticket.enums.BookingStatus;
import com.example.ticket.enums.SeatStatus;
import com.example.ticket.exception.SeatHeldByOtherUserException;
import com.example.ticket.exception.SeatHoldExpiredException;
import com.example.ticket.exception.SeatNotHeldException;
import com.example.ticket.exception.SeatUnavailableException;
import com.example.ticket.repository.BookingRepository;
import com.example.ticket.repository.BookingSeatRepository;
import com.example.ticket.repository.SeatRepository;
import com.example.ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final BookingSeatRepository bookingSeatRepo;
    private final SeatRepository seatRepo;
    private static final String SEAT_HOLD_KEY_PREFIX = "hold:seat:";
    private final RedisTemplate<String, Object> redisTemplate;


    @Transactional
    public BookingResponse confirmBooking(ConfirmBookingRequest request){
        Long userId = request.getUserId();

        List<Long> seatIds = request.getSeatIds();

        List<String> keysToDelete = new ArrayList<>();

        for (Long seatId : seatIds) {

            String key = SEAT_HOLD_KEY_PREFIX + seatId;

            Object holder = redisTemplate.opsForValue().get(key);

            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

            if (holder == null) {

                if (ttl != null && ttl == -2) {
                    throw new SeatHoldExpiredException("Hold expired for seat: " + seatId);
                }

                throw new SeatNotHeldException("Seat not held: " + seatId);
            }

            if (!holder.toString().equals(userId.toString())) {
                throw new SeatHeldByOtherUserException("Seat held by another user: " + seatId);
            }

            keysToDelete.add(key);
        }

        BookingResponse response = createBookingOptimistic(
                new CreateBookingRequest(userId, seatIds)
        );

        redisTemplate.delete(keysToDelete);

        return response;
    }

    @Transactional
    public BookingResponse createBookingOptimistic(CreateBookingRequest bookingRequest) {

        User user = userRepo.findById(bookingRequest.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user"));

        List<Long> sortedIds = bookingRequest.getSeatIds()
                .stream()
                .sorted()
                .toList();

        List<Seat> seats = seatRepo.findAllById(sortedIds);

        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatUnavailableException("Seat already booked");
            }
            seat.setStatus(SeatStatus.BOOKED);
        }

        return saveBooking(user, seats);
    }

    @Transactional
    public BookingResponse createBookingPessimistic(CreateBookingRequest bookingRequest)
            throws InterruptedException {

        User user = userRepo.findById(bookingRequest.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user"));

        List<Long> sortedIds = bookingRequest.getSeatIds()
                .stream()
                .sorted()
                .toList();

        List<Seat> seats = seatRepo.findAllByIdForUpdate(sortedIds);

        // để test concurrency
        Thread.sleep(3000);

        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatUnavailableException("Seat already booked");
            }
            seat.setStatus(SeatStatus.BOOKED);
        }

        return saveBooking(user, seats);
    }



    private BookingResponse saveBooking(User user, List<Seat> seats) {

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStatus(BookingStatus.PAID);
        booking.setBookingDate(LocalDateTime.now());

        BigDecimal totalAmount = seats.stream()
                .map(Seat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        booking.setTotalAmount(totalAmount);
        bookingRepo.save(booking);

        for (Seat seat : seats) {
            BookingSeat bookingSeat = new BookingSeat();
            bookingSeat.setBooking(booking);
            bookingSeat.setSeat(seat);
            bookingSeat.setPrice(seat.getPrice());
            bookingSeatRepo.save(bookingSeat);
        }

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .totalAmount(booking.getTotalAmount())
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus().name())
                .build();
    }
}
