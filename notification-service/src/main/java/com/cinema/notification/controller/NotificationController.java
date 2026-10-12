package com.cinema.notification.controller;

import com.cinema.notification.dto.NotificationResponse;
import com.cinema.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Đi qua Gateway: Gateway đã xác thực JWT và gắn X-User-Id
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationResponse> mine(@RequestHeader("X-User-Id") Long userId) {
        return service.listMine(userId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@RequestHeader("X-User-Id") Long userId) {
        return Map.of("count", service.unreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public NotificationResponse read(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return service.markRead(userId, id);
    }

    @PutMapping("/read-all")
    public Map<String, Integer> readAll(@RequestHeader("X-User-Id") Long userId) {
        return Map.of("updated", service.markAllRead(userId));
    }
}
