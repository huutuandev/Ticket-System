package com.example.ticket.service.seat;

import com.example.ticket.entity.Seat;
import com.example.ticket.enums.SeatStatus;
import com.example.ticket.repository.SeatRepository;
import com.example.ticket.service.SeatHoldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatHoldServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SeatHoldService seatHoldService;

    private Seat seat;

    @BeforeEach
    void setUp() {
        // Seat không có @Builder nên sử dụng setter
        seat = new Seat();
        seat.setId(10L);
        seat.setStatus(SeatStatus.HOLD);
        seat.setHoldByUserId(1L);
    }

    // Kiểm tra hold tồn tại
    @Test
    void isHeld_true() {
        when(redisTemplate.hasKey("hold:seat:10")).thenReturn(true);
        assertTrue(seatHoldService.isHeld("hold:seat:10"));
    }

    // Kiểm tra hold không tồn tại
    @Test
    void isHeld_false() {
        when(redisTemplate.hasKey("hold:seat:10")).thenReturn(false);
        assertFalse(seatHoldService.isHeld("hold:seat:10"));
    }

    // Kiểm tra hold hợp lệ cho user
    @Test
    void isHoldValidForUser_true() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hold:seat:10")).thenReturn("1");

        assertTrue(seatHoldService.isHoldValidForUser(10L, 1L));
    }

    // Kiểm tra hold không hợp lệ (thuộc về user khác)
    @Test
    void isHoldValidForUser_false() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hold:seat:10")).thenReturn("2"); // Thuộc user 2

        assertFalse(seatHoldService.isHoldValidForUser(10L, 1L));
    }
    
    // Kiểm tra hold không hợp lệ do không tồn tại trong redis
    @Test
    void isHoldValidForUser_null() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hold:seat:10")).thenReturn(null);

        assertFalse(seatHoldService.isHoldValidForUser(10L, 1L));
    }

    // Kiểm tra releaseHold một ghế thành công
    @Test
    void releaseHold_success() {
        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        
        seatHoldService.releaseHold("hold:seat:10");
        
        verify(redisTemplate, times(1)).delete("hold:seat:10");
        verify(seatRepository, times(1)).save(seat);
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        assertNull(seat.getHoldByUserId());
    }

    // Kiểm tra releaseHold một ghế nhưng ghế không ở trạng thái HOLD, không save lại db
    @Test
    void releaseHold_notHeldInDb() {
        seat.setStatus(SeatStatus.AVAILABLE);
        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        
        seatHoldService.releaseHold("hold:seat:10");
        
        verify(redisTemplate, times(1)).delete("hold:seat:10");
        verify(seatRepository, never()).save(any());
    }
    
    // Kiểm tra releaseHold với key sai format (không throw exception)
    @Test
    void releaseHold_invalidFormat() {
        seatHoldService.releaseHold("invalid_key");
        verify(redisTemplate, times(1)).delete("invalid_key");
        verify(seatRepository, never()).findById(anyLong());
    }

    // Kiểm tra releaseHolds danh sách ghế thành công
    @Test
    void releaseHolds_success() {
        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));
        
        seatHoldService.releaseHolds(List.of(10L));
        
        verify(redisTemplate, times(1)).delete("hold:seat:10");
        verify(seatRepository, times(1)).save(seat);
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        assertNull(seat.getHoldByUserId());
    }
    
    // Kiểm tra releaseHolds khi truyền null không lỗi
    @Test
    void releaseHolds_nullList() {
        seatHoldService.releaseHolds(null);
        verify(redisTemplate, never()).delete(anyString());
        verify(seatRepository, never()).findById(anyLong());
    }
}
