package com.example.ticket.controller;

import com.example.ticket.dto.request.HoldSeatRequest;
import com.example.ticket.dto.request.PaymentCreateRequest;
import com.example.ticket.dto.response.HoldSeatResponse;
import com.example.ticket.dto.response.PaymentResponse;
import com.example.ticket.entity.Booking;
import com.example.ticket.entity.Concert;
import com.example.ticket.entity.Seat;
import com.example.ticket.entity.User;
import com.example.ticket.enums.BookingStatus;
import com.example.ticket.enums.SeatStatus;
import com.example.ticket.enums.SeatType;
import com.example.ticket.repository.*;
import com.example.ticket.security.user.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private User user;
    private Concert concert;
    private Seat seat;

    @BeforeEach
    public void setUp() {
        // Clean database (order matters due to FK constraints)
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
        paymentRepository.deleteAll();
        seatRepository.deleteAll();
        concertRepository.deleteAll();
        userRepository.deleteAll();

        // Clean Redis
        try {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        } catch (Exception e) {
            // Ignore if connection fails
        }

        // Seed data
        user = new User();
        user.setUsername("testuser");
        user.setEmail("test@gmail.com");
        user.setFullName("Test User");
        user.setPassword("password");
        user = userRepository.save(user);

        concert = new Concert();
        concert.setName("Test Concert");
        concert.setLocation("Test Location");
        concert.setEventTime(LocalDateTime.now().plusDays(10));
        concert.setDescription("Test Description");
        concert = concertRepository.save(concert);

        seat = new Seat();
        seat.setRowName("A");
        seat.setSeatNumber(1L);
        seat.setPrice(BigDecimal.valueOf(100000));
        seat.setType(SeatType.STANDARD);
        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setConcert(concert);
        seat = seatRepository.save(seat);
    }

    @Test
    public void testHoldSeats_success() throws Exception {
        HoldSeatRequest holdReq = new HoldSeatRequest();
        holdReq.setSeatIds(List.of(seat.getId()));

        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        mockMvc.perform(post("/api/seats/hold")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.totalAmount").value(100000.0))
                .andExpect(jsonPath("$.result.heldSeats[0].seatId").value(seat.getId()))
                .andExpect(jsonPath("$.result.heldSeats[0].seatCode").value("A1"))
                .andExpect(jsonPath("$.result.heldSeats[0].price").value(100000.0));
    }

    @Test
    public void testFullFlow_success() throws Exception {
        // 1. Hold seat (Simulate holdSeats logic: set in Redis and DB status to HOLD)
        String holdId = "hold:seat:" + seat.getId();
        redisTemplate.opsForValue().set(holdId, user.getId().toString(), Duration.ofMinutes(5));

        seat.setStatus(SeatStatus.HOLD);
        seat.setHoldByUserId(user.getId());
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
        seat = seatRepository.save(seat);

        // 2. Create Payment
        PaymentCreateRequest createReq = PaymentCreateRequest.builder()
                .seatIds(List.of(seat.getId()))
                .userId(user.getId())
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        String createResContent = mockMvc.perform(post("/api/payments")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.status").value("PENDING"))
                .andExpect(jsonPath("$.result.seatIds[0]").value(seat.getId()))
                .andExpect(jsonPath("$.result.userId").value(user.getId()))
                .andExpect(jsonPath("$.result.amount").value(100000.0))
                .andReturn().getResponse().getContentAsString();

        PaymentResponse paymentResponse = objectMapper.readValue(
                objectMapper.readTree(createResContent).get("result").toString(),
                PaymentResponse.class
        );
        assertNotNull(paymentResponse.getId());

        // 3. Mock pay SUCCESS
        mockMvc.perform(post("/api/payments/" + paymentResponse.getId() + "/mock-pay?result=success")
                        .with(authentication(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.status").value("SUCCESS"));

        // 4. Assertions
        Seat updatedSeat = seatRepository.findById(seat.getId()).orElseThrow();
        assertEquals(SeatStatus.BOOKED, updatedSeat.getStatus());
        assertNull(updatedSeat.getHoldByUserId());

        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(holdId)));

        List<Booking> bookings = bookingRepository.findAll();
        assertEquals(1, bookings.size());
        assertEquals(user.getId(), bookings.get(0).getUser().getId());
        assertEquals(BookingStatus.PAID, bookings.get(0).getStatus());
    }

    @Test
    public void testFullFlow_failed() throws Exception {
        // 1. Hold seat
        String holdId = "hold:seat:" + seat.getId();
        redisTemplate.opsForValue().set(holdId, user.getId().toString(), Duration.ofMinutes(5));

        seat.setStatus(SeatStatus.HOLD);
        seat.setHoldByUserId(user.getId());
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
        seat = seatRepository.save(seat);

        // 2. Create Payment
        PaymentCreateRequest createReq = PaymentCreateRequest.builder()
                .seatIds(List.of(seat.getId()))
                .userId(user.getId())
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        String createResContent = mockMvc.perform(post("/api/payments")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        PaymentResponse paymentResponse = objectMapper.readValue(
                objectMapper.readTree(createResContent).get("result").toString(),
                PaymentResponse.class
        );

        // 3. Mock pay FAIL
        mockMvc.perform(post("/api/payments/" + paymentResponse.getId() + "/mock-pay?result=fail")
                        .with(authentication(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.status").value("FAILED"));

        // 4. Assertions
        Seat updatedSeat = seatRepository.findById(seat.getId()).orElseThrow();
        assertEquals(SeatStatus.AVAILABLE, updatedSeat.getStatus());
        assertNull(updatedSeat.getHoldByUserId());

        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(holdId)));

        List<Booking> bookings = bookingRepository.findAll();
        assertEquals(0, bookings.size());
    }
}
