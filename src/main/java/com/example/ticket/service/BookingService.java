package com.example.ticket.service;

import com.example.ticket.dto.request.ConfirmBookingRequest;
import com.example.ticket.dto.request.CreateBookingRequest;
import com.example.ticket.dto.response.BookingHistoryResponse;
import com.example.ticket.dto.response.BookingResponse;
import com.example.ticket.entity.Booking;
import com.example.ticket.entity.BookingSeat;
import com.example.ticket.entity.Seat;
import com.example.ticket.entity.User;
import com.example.ticket.enums.BookingStatus;
import com.example.ticket.enums.EmailType;
import com.example.ticket.enums.SeatStatus;
import com.example.ticket.event.BookingCreatedEvent;
import com.example.ticket.event.EmailEvent;
import com.example.ticket.exception.*;
import com.example.ticket.repository.BookingRepository;
import com.example.ticket.repository.BookingSeatRepository;
import com.example.ticket.repository.SeatRepository;
import com.example.ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final BookingSeatRepository bookingSeatRepo;
    private final SeatRepository seatRepo;
    private static final String SEAT_HOLD_KEY_PREFIX = "hold:seat:";
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher applicationEventPublisher;


    @Transactional
    public BookingResponse confirmBooking(ConfirmBookingRequest request, Long userId){
        log.info("Bắt đầu confirm booking cho userId {} với các seatIds {}", userId, request.getSeatIds());
        List<Long> seatIds = request.getSeatIds();

        LocalDateTime now =  LocalDateTime.now();

        List<String> keysToDelete = new ArrayList<>();

        List<Seat> seats = seatRepo.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new ResourceNotFoundException("Some seats not found");
        }

        Long concertId = seats.get(0).getConcert().getId();

        for (Seat seat : seats) {
            String key = SEAT_HOLD_KEY_PREFIX + seat.getId();
            Object redisHolder = redisTemplate.opsForValue().get(key);

            if (redisHolder != null) {
                if (!redisHolder.toString().equals(userId.toString())) {
                    throw new SeatHeldByOtherUserException("Seat held by another user: " + seat.getId());
                }
                keysToDelete.add(key);
            }
            else {
                if (seat.getStatus() != SeatStatus.HOLD
                        || !Objects.equals(seat.getHoldByUserId(), userId)
                        || seat.getHoldExpiresAt() == null
                        || seat.getHoldExpiresAt().isBefore(now)) {

                    throw new SeatHoldExpiredException("Hold expired or invalid for seat: " + seat.getId());
                }
                keysToDelete.add(key);
            }

            // Final safety check
            if (seat.getStatus() != SeatStatus.HOLD) {
                throw new SeatUnavailableException("Seat is not in HOLD state: " + seat.getId());
            }
        }

        BookingResponse response = createBookingOptimistic(
                new CreateBookingRequest(seatIds), userId
        );

        redisTemplate.delete(keysToDelete);
        log.debug("Đã xoá Redis hold keys: {}", keysToDelete);

        evictSeatCache(concertId);

        log.info("Confirm booking thành công cho userId {}", userId);
        return response;
    }

    @Transactional
    public BookingResponse createBookingOptimistic(CreateBookingRequest bookingRequest, Long userId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user"));

        List<Long> sortedIds = bookingRequest.getSeatIds()
                .stream()
                .sorted()
                .toList();

        List<Seat> seats = seatRepo.findAllById(sortedIds);

        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.HOLD) {
                throw new SeatUnavailableException(
                        "Seat is not in HOLD state: " + seat.getId()
                );
            }
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHoldByUserId(null);
            seat.setHoldExpiresAt(null);
        }

        BookingResponse response = saveBooking(user, seats);

        // === PUBLISH EVENT ===
        String concertName = seats.get(0).getConcert().getName();
        String seatsStr = seats.stream()
                .map(s -> s.getRowName() + s.getSeatNumber())
                .collect(java.util.stream.Collectors.joining(", "));

        BookingCreatedEvent event = new BookingCreatedEvent(
                response.getBookingId(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                concertName,
                seats.get(0).getConcert().getEventTime(),
                seatsStr,
                response.getTotalAmount(),
                response.getBookingDate()
        );

        applicationEventPublisher.publishEvent(event);

        return response;
    }

    @Transactional
    public BookingResponse createBookingPessimistic(CreateBookingRequest bookingRequest, Long userId)
            throws InterruptedException {

        User user = userRepo.findById(userId)
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

    private void evictSeatCache(Long concertId) {
        if (concertId != null) {
            Cache cache = cacheManager.getCache("concert-seats");
            if (cache != null) {
                cache.evict(concertId);
//                log.info("Cache evicted for concertId: {}", concertId);
            }
        }
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

    @Transactional(readOnly = true)
    public Page<BookingHistoryResponse> getBookingHistory(Long userId, String status, Pageable pageable) {
        Page<Booking> bookings;

        if (status != null && !status.trim().isEmpty()) {
            try {
                BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
                bookings = bookingRepo.findByUserIdAndStatusWithDetails(userId, bookingStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
        } else {
            bookings = bookingRepo.findByUserIdWithDetails(userId, pageable);
        }

        return bookings.map(this::mapToHistoryResponse);
    }

    private BookingHistoryResponse mapToHistoryResponse(Booking booking) {
        BookingHistoryResponse response = new BookingHistoryResponse();
        response.setBookingId(booking.getId());
        response.setTotalAmount(booking.getTotalAmount());
        response.setStatus(booking.getStatus().name());
        response.setCreatedAt(booking.getBookingDate());

        if (!booking.getBookingSeats().isEmpty()) {
            Seat firstSeat = booking.getBookingSeats().iterator().next().getSeat();
            response.setConcertId(firstSeat.getConcert().getId());
            response.setConcertName(firstSeat.getConcert().getName());
            response.setShowTime(firstSeat.getConcert().getEventTime());
            response.setPosterUrl(firstSeat.getConcert().getImageUrl());
        }

        List<BookingHistoryResponse.SeatInfo> seatInfos = booking.getBookingSeats().stream()
                .map(bs -> new BookingHistoryResponse.SeatInfo(
                        bs.getSeat().getRowName() + bs.getSeat().getSeatNumber(),
                        bs.getPrice()
                ))
                .toList();

        response.setSeats(seatInfos);
        return response;
    }
}
