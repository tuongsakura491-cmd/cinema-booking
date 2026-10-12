package com.cinema.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotNull Long userId,
        @NotBlank String type,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 500) String content,
        String dedupeKey,
        String expiresAt) {   // ISO-8601, VD 2026-10-11T19:30:00
}
