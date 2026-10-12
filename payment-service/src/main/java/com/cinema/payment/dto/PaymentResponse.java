package com.cinema.payment.dto;

import java.time.LocalDateTime;

public record PaymentResponse(Long id, Long bookingId, Long amount, String method,
                              String status, String transactionCode, LocalDateTime createdAt) {
}
