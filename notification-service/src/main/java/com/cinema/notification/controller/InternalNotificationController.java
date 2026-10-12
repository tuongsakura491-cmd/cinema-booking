package com.cinema.notification.controller;

import com.cinema.notification.dto.CreateNotificationRequest;
import com.cinema.notification.dto.NotificationResponse;
import com.cinema.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Chỉ Booking Service gọi (Gateway không route /internal/**)
@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {
    private final NotificationService service;
    private final String internalToken;

    public InternalNotificationController(NotificationService service,
                                          @Value("${internal.token}") String internalToken) {
        this.service = service;
        this.internalToken = internalToken;
    }

    @PostMapping
    public NotificationResponse create(@Valid @RequestBody CreateNotificationRequest req,
                                       @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (!internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
        }
        return service.create(req);
    }
}
