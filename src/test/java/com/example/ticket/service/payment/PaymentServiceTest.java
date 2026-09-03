package com.example.ticket.service.payment;

import com.example.ticket.dto.request.ConfirmBookingRequest;
import com.example.ticket.dto.request.PaymentCreateRequest;
import com.example.ticket.dto.response.PaymentResponse;
import com.example.ticket.entity.Payment;
import com.example.ticket.entity.Seat;
import com.example.ticket.entity.User;
import com.example.ticket.enums.MockResult;
import com.example.ticket.enums.PaymentStatus;
import com.example.ticket.exception.InvalidPaymentStateException;
import com.example.ticket.exception.PaymentNotFoundException;
import com.example.ticket.exception.ResourceNotFoundException;
import com.example.ticket.exception.SeatHoldExpiredException;
import com.example.ticket.repository.PaymentRepository;
import com.example.ticket.repository.SeatRepository;
import com.example.ticket.service.BookingService;
import com.example.ticket.service.PaymentService;
import com.example.ticket.service.SeatHoldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SeatHoldService seatHoldService;

    @Mock
    private BookingService bookingService;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment payment;

    private User user;

    @BeforeEach
    public void setUp() {
        payment = Payment.builder()
                .id(1L)
                .holdId("hold:seats:[100]")
                .seatIds(List.of(100L))
                .userId(10L)
                .amount(BigDecimal.valueOf(150000))
                .status(PaymentStatus.PENDING)
                .transactionId("tx-12345")
                .createdAt(LocalDateTime.now())
                .build();

        user = User.builder()
                .id(10L)
                .email("tuan@gmail.com")
                .fullName("Nguyễn Hữu Tuấn")
                .status("ACTIVE")
                .build();
    }


    @Test
    public void createPayment_success() {
        PaymentCreateRequest req = PaymentCreateRequest.builder()
                .seatIds(List.of(100L))
                .build();

        Seat seat = new Seat();
        seat.setId(100L);
        seat.setPrice(BigDecimal.valueOf(150000));

        when(seatHoldService.isHoldValidForUser(100L, 10L)).thenReturn(true);
        when(seatRepository.findAllById(List.of(100L))).thenReturn(List.of(seat));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.createPayment(req, user.getId());

        assertEquals("PENDING", response.getStatus());
        assertEquals(BigDecimal.valueOf(150000), response.getAmount());
        assertEquals(List.of(100L), response.getSeatIds());
        verify(seatHoldService, times(1)).isHoldValidForUser(100L, 10L);
    }

    @Test
    public void createPayment_holdExpired() {
        PaymentCreateRequest req = PaymentCreateRequest.builder()
                .seatIds(List.of(100L))
                .build();

        when(seatHoldService.isHoldValidForUser(100L, 10L)).thenReturn(false);

        assertThrows(SeatHoldExpiredException.class, () -> paymentService.createPayment(req, user.getId()));
    }

    @Test
    public void createPayment_seatNotFound() {
        PaymentCreateRequest req = PaymentCreateRequest.builder()
                .seatIds(List.of(100L))
                .build();

        when(seatHoldService.isHoldValidForUser(100L, 10L)).thenReturn(true);
        when(seatRepository.findAllById(List.of(100L))).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.createPayment(req, user.getId()));
    }

    @Test
    public void mockPay_success() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.mockPay(1L, MockResult.SUCCESS);

        assertEquals("SUCCESS", response.getStatus());
        verify(paymentRepository, times(1)).save(payment);

        ArgumentCaptor<ConfirmBookingRequest> reqCaptor = ArgumentCaptor.forClass(ConfirmBookingRequest.class);
        verify(bookingService, times(1)).confirmBooking(reqCaptor.capture(), eq(10L));
        assertEquals(1, reqCaptor.getValue().getSeatIds().size());
        assertEquals(100L, reqCaptor.getValue().getSeatIds().get(0));
    }

    @Test
    public void mockPay_failed() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.mockPay(1L, MockResult.FAILED);

        assertEquals("FAILED", response.getStatus());
        verify(seatHoldService, times(1)).releaseHolds(List.of(100L));
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    public void mockPay_confirmBooking_throwsException() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        doThrow(new RuntimeException("Booking fail")).when(bookingService).confirmBooking(any(), any());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(RuntimeException.class, () -> paymentService.mockPay(1L, MockResult.SUCCESS));

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(seatHoldService, times(1)).releaseHolds(List.of(100L));
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    public void mockPay_invalidState_throwsException() {
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(InvalidPaymentStateException.class, () -> paymentService.mockPay(1L, MockResult.SUCCESS));
    }

    @Test
    public void mockPay_notFound_throwsException() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.mockPay(1L, MockResult.SUCCESS));
    }
}
