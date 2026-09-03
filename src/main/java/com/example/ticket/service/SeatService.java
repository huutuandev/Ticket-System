package com.example.ticket.service;


import com.example.ticket.dto.request.GenerateSeatsRequest;
import com.example.ticket.dto.request.HoldSeatRequest;
import com.example.ticket.dto.response.SeatHoldStatusResponse;
import com.example.ticket.dto.response.SeatResponse;
import com.example.ticket.dto.response.HoldSeatResponse;
import com.example.ticket.entity.Concert;
import com.example.ticket.entity.Seat;
import com.example.ticket.enums.SeatStatus;
import com.example.ticket.enums.SeatType;
import com.example.ticket.exception.ResourceNotFoundException;
import com.example.ticket.exception.SeatAlreadyHeldException;
import com.example.ticket.exception.SeatUnavailableException;
import com.example.ticket.repository.ConcertRepository;
import com.example.ticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {
    private final ConcertRepository concertRepo;
    private final SeatRepository seatRepo;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;
    private static final String SEAT_HOLD_KEY_PREFIX = "hold:seat:";
    private static final String SEAT_CACHE = "concert-seats";


    @CacheEvict(value = SEAT_CACHE, key = "#concertId")
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

    @Cacheable(value = "concert-seats", key = "#concertId")
    public List<SeatResponse> getSeatsByConcert(Long concertId) {
        System.out.println("🔥 DB HIT");
        List<Seat> seats = seatRepo.findByConcertId(concertId);
        return seats.stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional
    public HoldSeatResponse holdSeats(HoldSeatRequest request, Long userId) {

        List<Seat> seats = seatRepo.findAllById(request.getSeatIds());

        if(seats.size() != request.getSeatIds().size()){
            throw new ResourceNotFoundException("Seat not found");
        }
        Long concertId = seats.get(0).getConcert().getId();

        List<String> lockedKeys = new ArrayList<>();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(5);

        try {
            for (Seat seat : seats) {

                if (seat.getStatus() != SeatStatus.AVAILABLE) {
                    throw new SeatUnavailableException("Seat not available: " + seat.getId());
                }

                seat.setStatus(SeatStatus.HOLD);
                seat.setHoldByUserId(userId);
                seat.setHoldExpiresAt(expireTime);

                String key = SEAT_HOLD_KEY_PREFIX + seat.getId();

                Boolean success = redisTemplate.opsForValue().setIfAbsent(
                        key,
                        userId,
                        Duration.ofMinutes(5)
                );

                if (Boolean.FALSE.equals(success)) {
                    log.warn("Hold seat thất bại (Redis SETNX). Key: {}, UserId: {}", key, userId);
                    throw new SeatAlreadyHeldException("Seat already held: " + seat.getId());
                } else {
                    log.debug("Hold seat thành công (Redis SETNX). Key: {}, UserId: {}, TTL: 5 minutes", key, userId);
                }

                lockedKeys.add(key);
            }
            seatRepo.saveAll(seats);

            if (concertId != null) {
                Cache cache = cacheManager.getCache("concert-seats");
                if (cache != null) {
                    cache.evict(concertId);
                }
            }

            List<HoldSeatResponse.HeldSeatDto> heldSeatDtos = seats.stream()
                    .map(seat -> HoldSeatResponse.HeldSeatDto.builder()
                            .seatId(seat.getId())
                            .seatCode(seat.getRowName() + seat.getSeatNumber())
                            .price(seat.getPrice())
                            .build())
                    .toList();

            BigDecimal totalAmount = seats.stream()
                    .map(Seat::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return HoldSeatResponse.builder()
                    .heldSeats(heldSeatDtos)
                    .totalAmount(totalAmount)
                    .holdExpiresAt(expireTime)
                    .build();

        } catch (Exception e) {
            log.error("Lỗi khi holdSeats cho userId {}, message: {}", userId, e.getMessage(), e);
            // rollback Redis locks
            for (String key : lockedKeys) {
                redisTemplate.delete(key);
                log.debug("Rollback Redis lock key: {}", key);
            }

            throw e;
        }
    }

    public SeatHoldStatusResponse getHoldStatus(Long seatId){

        String key = SEAT_HOLD_KEY_PREFIX + seatId;

        Object holder = redisTemplate.opsForValue().get(key);

        if(holder == null){
            return new SeatHoldStatusResponse(seatId, false, 0L);
        }

        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        return SeatHoldStatusResponse.builder()
                .seatId(seatId)
                .held(true)
                .remainingSeconds(ttl)
                .build();

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
