package com.cinema.booking.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Gọi Notification Service. Ném RestClientException nếu lỗi: người gọi tự quyết định xử lý. */
@Component
public class NotificationClient {
    private final RestClient client;
    private final String token;

    public NotificationClient(@Value("${notification.url}") String url,
                              @Value("${internal.token}") String token) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
        this.token = token;
    }

    public void send(Long userId, String type, String title, String content,
                     String dedupeKey, LocalDateTime expiresAt) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("type", type);
        body.put("title", title);
        body.put("content", content);
        body.put("dedupeKey", dedupeKey);
        body.put("expiresAt", expiresAt == null ? null : expiresAt.toString()); // ISO-8601; null means no expiry

        client.post()
                .uri("/internal/notifications")
                .header("X-Internal-Token", token)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
