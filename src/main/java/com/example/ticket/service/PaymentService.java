package com.example.ticket.service;

import com.example.ticket.dto.request.ConfirmBookingRequest;
import com.example.ticket.dto.request.PaymentCreateRequest;
import com.example.ticket.dto.response.PaymentResponse;
import com.example.ticket.entity.Payment;
import com.example.ticket.entity.Seat;
import com.example.ticket.enums.MockResult;
import com.example.ticket.enums.PaymentStatus;
import com.example.ticket.exception.InvalidPaymentStateException;
import com.example.ticket.exception.PaymentNotFoundException;
import com.example.ticket.exception.ResourceNotFoundException;
import com.example.ticket.exception.SeatHoldExpiredException;
import com.example.ticket.repository.PaymentRepository;
import com.example.ticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SeatHoldService seatHoldService;
    private final BookingService bookingService;
    private final SeatRepository seatRepository;

    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest req, Long userId) {
        log.info("Bắt đầu tạo payment cho userId {} với các seatIds {}", userId, req.getSeatIds());
        // Verify Redis holds exist and belong to the correct user for all seats
        for (Long seatId : req.getSeatIds()) {
            if (!seatHoldService.isHoldValidForUser(seatId, userId)) {
                throw new SeatHoldExpiredException("Hold expired or invalid for seat: " + seatId);
            }
        }

        // Retrieve seats from PostgreSQL to calculate the amount on the server-side
        List<Seat> seats = seatRepository.findAllById(req.getSeatIds());
        if (seats.size() != req.getSeatIds().size()) {
            throw new ResourceNotFoundException("Some seats not found");
        }

        String holdId = "hold:seats:" + req.getSeatIds().toString();

        Payment payment = Payment.builder()
                .holdId(holdId)
                .seatIds(req.getSeatIds())
                .userId(userId)
                .amount(req.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .transactionId(UUID.randomUUID().toString())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Tạo payment thành công, paymentId: {}", savedPayment.getId());
        return toResponse(savedPayment);
    }

    // We purposely do not annotate the overall method with @Transactional.
    // This ensures that when bookingService.confirmBooking throws an exception and rolls back its transaction,
    // we can still catch the exception and successfully commit the updates to the payment status (to FAILED)
    // and release the seat hold in their own separate transactions.
    public PaymentResponse mockPay(Long paymentId, MockResult result) {
        log.info("Bắt đầu mockPay cho paymentId {}, result = {}", paymentId, result);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));

        log.debug("Trạng thái payment hiện tại: {}", payment.getStatus());
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Payment {} có trạng thái không hợp lệ: {}", paymentId, payment.getStatus());
            throw new InvalidPaymentStateException("Payment status is not PENDING. Current: " + payment.getStatus());
        }

        if (result == MockResult.SUCCESS) {
            try {
                log.info("Thực hiện confirm booking cho payment {} (userId: {}, seats: {})", paymentId, payment.getUserId(), payment.getSeatIds());
                ConfirmBookingRequest confirmReq = new ConfirmBookingRequest(payment.getSeatIds());
                bookingService.confirmBooking(confirmReq, payment.getUserId());
                payment.setStatus(PaymentStatus.SUCCESS);
                log.info("Payment {} chuyển sang trạng thái SUCCESS", paymentId);
            } catch (Exception e) {
                log.error("Confirm booking thất bại cho payment {}: {}", paymentId, e.getMessage(), e);
                payment.setStatus(PaymentStatus.FAILED);
                seatHoldService.releaseHolds(payment.getSeatIds());
                paymentRepository.save(payment);
                throw e;
            }
        } else if (result == MockResult.FAILED) {
            payment.setStatus(PaymentStatus.FAILED);
            seatHoldService.releaseHolds(payment.getSeatIds());
            log.info("Payment {} chuyển sang trạng thái FAILED do mock failed", paymentId);
        }

        Payment savedPayment = paymentRepository.save(payment);
        return toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .seatIds(payment.getSeatIds())
                .userId(payment.getUserId())
                .holdId(payment.getHoldId())
                .build();
    }
}
