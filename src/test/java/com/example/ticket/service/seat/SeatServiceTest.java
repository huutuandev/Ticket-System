package com.example.ticket.service.seat;

import com.example.ticket.dto.request.GenerateSeatsRequest;
import com.example.ticket.dto.request.HoldSeatRequest;
import com.example.ticket.dto.response.HoldSeatResponse;
import com.example.ticket.dto.response.SeatHoldStatusResponse;
import com.example.ticket.dto.response.SeatResponse;
import com.example.ticket.entity.Concert;
import com.example.ticket.entity.Seat;
import com.example.ticket.enums.SeatStatus;
import com.example.ticket.enums.SeatType;
import com.example.ticket.exception.SeatAlreadyHeldException;
import com.example.ticket.exception.SeatUnavailableException;
import com.example.ticket.repository.ConcertRepository;
import com.example.ticket.repository.SeatRepository;
import com.example.ticket.service.SeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatServiceTest {

    @Mock
    private ConcertRepository concertRepo;
    @Mock
    private SeatRepository seatRepo;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private Cache cache;

    @InjectMocks
    private SeatService seatService;

    private Concert concert;
    private Seat seat;

    @BeforeEach
    void setUp() {
        // Concert không có @Builder nên sử dụng setter
        concert = new Concert();
        concert.setId(100L);
        
        // Seat không có @Builder nên sử dụng setter
        seat = new Seat();
        seat.setId(1L);
        seat.setRowName("A");
        seat.setSeatNumber(1L);
        seat.setPrice(BigDecimal.valueOf(100000));
        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setType(SeatType.STANDARD);
        seat.setConcert(concert);
    }

    // Kiểm tra tạo danh sách ghế cho concert thành công
    @Test
    void generateSeats_success() {
        // GenerateSeatsRequest không có @Builder nên sử dụng setter. 
        // Phải truyền Long vì field là kiểu Long.
        GenerateSeatsRequest req = new GenerateSeatsRequest();
        req.setRows(2L);
        req.setSeatsPerRow(5L);

        when(concertRepo.findById(100L)).thenReturn(Optional.of(concert));
        when(seatRepo.existsByConcertId(100L)).thenReturn(false);
        when(seatRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<SeatResponse> responses = seatService.generateSeats(req, 100L);

        assertEquals(10, responses.size()); // 2 hàng * 5 ghế
        verify(seatRepo, times(1)).saveAll(anyList());
    }

    // Kiểm tra tạo ghế lỗi do vượt quá số hàng tối đa (26)
    @Test
    void generateSeats_tooManyRows_throwsException() {
        GenerateSeatsRequest req = new GenerateSeatsRequest();
        req.setRows(27L);
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> seatService.generateSeats(req, 100L));
        assertEquals("Maximum rows is 26", ex.getMessage());
        verify(concertRepo, never()).findById(anyLong());
    }
    
    // Kiểm tra tạo ghế lỗi do concert đã có ghế rồi
    @Test
    void generateSeats_seatsExist_throwsException() {
        GenerateSeatsRequest req = new GenerateSeatsRequest();
        req.setRows(2L);
        when(concertRepo.findById(100L)).thenReturn(Optional.of(concert));
        when(seatRepo.existsByConcertId(100L)).thenReturn(true);
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> seatService.generateSeats(req, 100L));
        assertEquals("Concert đã được tạo ghế", ex.getMessage());
    }

    // Kiểm tra lấy danh sách ghế theo concertId (db hit)
    @Test
    void getSeatsByConcert_success() {
        when(seatRepo.findByConcertId(100L)).thenReturn(List.of(seat));
        
        List<SeatResponse> responses = seatService.getSeatsByConcert(100L);
        
        assertEquals(1, responses.size());
        assertEquals("A", responses.get(0).getRowName());
    }

    // Kiểm tra giữ ghế thành công (holdSeats), lưu Redis và DB, xoá cache
    @Test
    void holdSeats_success() {
        // HoldSeatRequest không có @Builder nên sử dụng setter
        HoldSeatRequest req = new HoldSeatRequest();
        req.setSeatIds(List.of(1L));

        when(seatRepo.findAllById(req.getSeatIds())).thenReturn(List.of(seat));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("hold:seat:1"), eq(10L), any(Duration.class))).thenReturn(true);
        when(cacheManager.getCache("concert-seats")).thenReturn(cache);

        HoldSeatResponse response = seatService.holdSeats(req, 10L);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(100000), response.getTotalAmount());
        assertEquals(SeatStatus.HOLD, seat.getStatus());
        assertEquals(10L, seat.getHoldByUserId());
        
        verify(seatRepo, times(1)).saveAll(anyList());
        verify(cache, times(1)).evict(100L);
    }

    // Kiểm tra giữ ghế thất bại do ghế không AVAILABLE
    @Test
    void holdSeats_seatNotAvailable_throwsException() {
        HoldSeatRequest req = new HoldSeatRequest();
        req.setSeatIds(List.of(1L));
        seat.setStatus(SeatStatus.BOOKED);

        when(seatRepo.findAllById(req.getSeatIds())).thenReturn(List.of(seat));

        assertThrows(SeatUnavailableException.class, () -> seatService.holdSeats(req, 10L));
        verify(seatRepo, never()).saveAll(anyList());
    }
    
    // Kiểm tra giữ ghế thất bại do redis khóa thất bại (người khác đã khóa)
    @Test
    void holdSeats_redisSetIfAbsentFailed_throwsException() {
        HoldSeatRequest req = new HoldSeatRequest();
        req.setSeatIds(List.of(1L));

        when(seatRepo.findAllById(req.getSeatIds())).thenReturn(List.of(seat));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("hold:seat:1"), eq(10L), any(Duration.class))).thenReturn(false);

        assertThrows(SeatAlreadyHeldException.class, () -> seatService.holdSeats(req, 10L));
        // Verify rollback keys
        verify(redisTemplate, times(1)).delete("hold:seat:1");
    }

    // Kiểm tra trạng thái ghế đang được giữ (getHoldStatus)
    @Test
    void getHoldStatus_held() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hold:seat:1")).thenReturn("10"); // held by user 10
        when(redisTemplate.getExpire("hold:seat:1", TimeUnit.SECONDS)).thenReturn(150L);

        SeatHoldStatusResponse response = seatService.getHoldStatus(1L);

        assertTrue(response.isHeld());
        assertEquals(150L, response.getRemainingSeconds());
    }

    // Kiểm tra trạng thái ghế chưa bị ai giữ
    @Test
    void getHoldStatus_notHeld() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hold:seat:1")).thenReturn(null);

        SeatHoldStatusResponse response = seatService.getHoldStatus(1L);

        assertFalse(response.isHeld());
        assertEquals(0L, response.getRemainingSeconds());
    }
}
