package com.cinema.booking.client;

public record PaymentResult(Long paymentId, String status, String message, String transactionCode) {
}
