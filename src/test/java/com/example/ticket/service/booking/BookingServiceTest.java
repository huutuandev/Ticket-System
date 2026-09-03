package com.example.ticket.service.booking;

import com.example.ticket.dto.request.ConfirmBookingRequest;
import com.example.ticket.dto.request.CreateBookingRequest;
import com.example.ticket.dto.response.BookingHistoryResponse;
import com.example.ticket.dto.response.BookingResponse;
import com.example.ticket.entity.Booking;
import com.example.ticket.entity.BookingSeat;
import com.example.ticket.entity.Concert;
import com.example.ticket.entity.Seat;
import com.example.ticket.entity.User;
import com.example.ticket.enums.BookingStatus;
import com.example.ticket.enums.SeatStatus;
import com.example.ticket.event.BookingCreatedEvent;
import com.example.ticket.exception.ResourceNotFoundException;
import com.example.ticket.exception.SeatHoldExpiredException;
import com.example.ticket.exception.SeatUnavailableException;
import com.example.ticket.exception.SeatHeldByOtherUserException;
import com.example.ticket.repository.BookingRepository;
import com.example.ticket.repository.BookingSeatRepository;
import com.example.ticket.repository.SeatRepository;
import com.example.ticket.repository.UserRepository;
import com.example.ticket.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private BookingSeatRepository bookingSeatRepo;
    @Mock
    private SeatRepository seatRepo;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private Cache cache;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private Seat seat;
    private Concert concert;

    @BeforeEach
    void setUp() {
        // User có @Builder nên dùng builder
        user = User.builder()
                .id(10L)
                .email("test@example.com")
                .build();

        // Concert không có @Builder nên dùng setter
        concert = new Concert();
        concert.setId(100L);
        concert.setName("Test Concert");
        concert.setEventTime(LocalDateTime.now().plusDays(5));

        // Seat không có @Builder nên dùng setter
        seat = new Seat();
        seat.setId(1L);
        seat.setStatus(SeatStatus.HOLD);
        seat.setHoldByUserId(10L);
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
        seat.setConcert(concert);
        seat.setPrice(BigDecimal.valueOf(100000));
        seat.setRowName("A");
        seat.setSeatNumber(1L);
    }

    // Kiểm tra xác nhận booking thành công: xóa hold, lưu booking và publish sự kiện, xóa cache
    @Test
    void confirmBooking_success() {
        // ConfirmBookingRequest không có @Builder nên dùng setter
        ConfirmBookingRequest req = new ConfirmBookingRequest();
        req.setSeatIds(List.of(1L));

        when(seatRepo.findAllById(anyList())).thenReturn(List.of(seat));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hold:seat:1")).thenReturn("10"); // user id
        when(userRepo.findById(10L)).thenReturn(Optional.of(user));
        when(bookingRepo.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("concert-seats")).thenReturn(cache);

        BookingResponse response = bookingService.confirmBooking(req, 10L);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(100000), response.getTotalAmount());
        
        verify(redisTemplate, times(1)).delete(anyList());
        verify(cache, times(1)).evict(100L);
        verify(applicationEventPublisher, times(1)).publishEvent(any(BookingCreatedEvent.class));
        assertEquals(SeatStatus.BOOKED, seat.getStatus());
    }

    // Kiểm tra confirm booking thất bại khi số lượng seat không khớp
    @Test
    void confirmBooking_seatNotFound_throwsException() {
        ConfirmBookingRequest req = new ConfirmBookingRequest();
        req.setSeatIds(List.of(1L, 2L));

        when(seatRepo.findAllById(anyList())).thenReturn(List.of(seat)); // Chỉ tìm thấy 1 seat

        assertThrows(ResourceNotFoundException.class, () -> bookingService.confirmBooking(req, 10L));
    }

    // Kiểm tra confirm booking thất bại do bị hold bởi user khác
    @Test
    void confirmBooking_seatHeldByOtherUser_throwsException() {
        ConfirmBookingRequest req = new ConfirmBookingRequest();
        req.setSeatIds(List.of(1L));
        
        when(seatRepo.findAllById(anyList())).thenReturn(List.of(seat));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hold:seat:1")).thenReturn("99"); // held by another user
        
        assertThrows(SeatHeldByOtherUserException.class, () -> bookingService.confirmBooking(req, 10L));
    }

    // Kiểm tra confirm booking thất bại do hold quá hạn hoặc bị user khác hold (nếu redis null)
    @Test
    void confirmBooking_holdExpired_throwsException() {
        ConfirmBookingRequest req = new ConfirmBookingRequest();
        req.setSeatIds(List.of(1L));
        
        seat.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1)); // Đã hết hạn

        when(seatRepo.findAllById(anyList())).thenReturn(List.of(seat));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hold:seat:1")).thenReturn(null); // Không có trong redis
        
        assertThrows(SeatHoldExpiredException.class, () -> bookingService.confirmBooking(req, 10L));
    }
    
    // Kiểm tra tạo booking optimistic khi trạng thái ghế không phải HOLD
    @Test
    void createBookingOptimistic_seatNotHold_throwsException() {
        // CreateBookingRequest không có @Builder, có AllArgsConstructor
        CreateBookingRequest req = new CreateBookingRequest(List.of(1L));
        seat.setStatus(SeatStatus.AVAILABLE);
        
        when(userRepo.findById(10L)).thenReturn(Optional.of(user));
        when(seatRepo.findAllById(anyList())).thenReturn(List.of(seat));
        
        assertThrows(SeatUnavailableException.class, () -> bookingService.createBookingOptimistic(req, 10L));
    }
    
    // Kiểm tra tạo booking pessimistic thành công (mặc dù có theard.sleep test, ở đây sẽ mô phỏng)
    @Test
    void createBookingPessimistic_success() throws InterruptedException {
        CreateBookingRequest req = new CreateBookingRequest(List.of(1L));
        seat.setStatus(SeatStatus.AVAILABLE);
        
        when(userRepo.findById(10L)).thenReturn(Optional.of(user));
        when(seatRepo.findAllByIdForUpdate(anyList())).thenReturn(List.of(seat));
        when(bookingRepo.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = bookingService.createBookingPessimistic(req, 10L);

        assertNotNull(response);
        assertEquals(SeatStatus.BOOKED, seat.getStatus());
        verify(bookingRepo, times(1)).save(any(Booking.class));
    }

    // Kiểm tra lấy lịch sử booking thành công
    @Test
    void getBookingHistory_success() {
        // Booking không có @Builder
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setTotalAmount(BigDecimal.valueOf(100000));
        booking.setStatus(BookingStatus.PAID);
        booking.setBookingDate(LocalDateTime.now());
        booking.setBookingSeats(new HashSet<>());
        
        // BookingSeat không có @Builder
        BookingSeat bookingSeat = new BookingSeat();
        bookingSeat.setSeat(seat);
        bookingSeat.setPrice(BigDecimal.valueOf(100000));
        booking.getBookingSeats().add(bookingSeat);

        Page<Booking> page = new PageImpl<>(List.of(booking));
        when(bookingRepo.findByUserIdWithDetails(eq(10L), any())).thenReturn(page);

        Page<BookingHistoryResponse> result = bookingService.getBookingHistory(10L, null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getConcertId());
        assertEquals("Test Concert", result.getContent().get(0).getConcertName());
        verify(bookingRepo, times(1)).findByUserIdWithDetails(eq(10L), any());
    }
    
    // Kiểm tra lấy lịch sử booking có filter status
    @Test
    void getBookingHistory_withStatus_success() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.PAID);
        booking.setBookingSeats(new HashSet<>());
        
        Page<Booking> page = new PageImpl<>(List.of(booking));
        when(bookingRepo.findByUserIdAndStatusWithDetails(eq(10L), eq(BookingStatus.PAID), any())).thenReturn(page);

        Page<BookingHistoryResponse> result = bookingService.getBookingHistory(10L, "PAID", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        verify(bookingRepo, times(1)).findByUserIdAndStatusWithDetails(eq(10L), eq(BookingStatus.PAID), any());
    }
}
