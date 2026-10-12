package com.cinema.payment.dto;

public record PayResult(Long paymentId, String status, String message, String transactionCode) {
}
