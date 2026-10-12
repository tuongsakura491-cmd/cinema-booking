package com.cinema.payment.controller;

import com.cinema.payment.dto.PayRequest;
import com.cinema.payment.dto.PayResult;
import com.cinema.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// Chỉ Booking Service gọi (Gateway không route /internal/**)
@RestController
@RequestMapping("/internal/payments")
public class InternalPaymentController {
    private final PaymentService service;
    private final String internalToken;

    public InternalPaymentController(PaymentService service, @Value("${internal.token}") String internalToken) {
        this.service = service;
        this.internalToken = internalToken;
    }

    @PostMapping("/pay")
    public PayResult pay(@Valid @RequestBody PayRequest req,
                         @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        check(token);
        return service.pay(req);
    }

    @PostMapping("/bookings/{bookingId}/refund")
    public Map<String, String> refund(@PathVariable Long bookingId,
                                      @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        check(token);
        return Map.of("status", service.refund(bookingId));
    }

    private void check(String token) {
        if (!internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
        }
    }
}
