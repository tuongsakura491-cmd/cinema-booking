package com.cinema.payment.controller;

import com.cinema.payment.dto.PaymentResponse;
import com.cinema.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Đi qua Gateway: Gateway đã xác thực JWT và gắn X-User-Id
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/my")
    public List<PaymentResponse> mine(@RequestHeader("X-User-Id") Long userId) {
        return service.listMine(userId);
    }
}
