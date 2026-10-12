package com.cinema.booking.client;

import com.cinema.booking.dto.PayInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentClient {
    private final RestClient client;
    private final String token;

    public PaymentClient(@Value("${payment.url}") String url, @Value("${internal.token}") String token) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
        this.token = token;
    }

    /** Thanh toán. An toàn khi gọi lại (Payment Service idempotent theo bookingId). */
    public PaymentResult pay(Long bookingId, Long userId, Long amount, PayInfo info) {
        Map<String, Object> body = new HashMap<>();
        body.put("bookingId", bookingId);
        body.put("userId", userId);
        body.put("amount", amount);                        // lấy từ booking_db, không từ client
        body.put("method", info == null ? null : info.method());
        body.put("cardLast4", info == null ? null : info.cardLast4());
        try {
            return client.post()
                    .uri("/internal/payments/pay")
                    .header("X-Internal-Token", token)
                    .body(body)
                    .retrieve()
                    .body(PaymentResult.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 400) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thông tin thanh toán không hợp lệ");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment Service lỗi: " + e.getStatusCode());
        } catch (RestClientException e) {
            // Có thể Payment đã xử lý xong mà phản hồi bị chậm: bấm thanh toán lại là an toàn
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment Service không phản hồi, vui lòng thử lại (hệ thống không trừ tiền hai lần)");
        }
    }

    /** Hoàn tiền, idempotent. Ném lỗi nếu không hoàn được để người gọi quyết định. */
    public void refund(Long bookingId) {
        try {
            client.post()
                    .uri("/internal/payments/bookings/{id}/refund", bookingId)
                    .header("X-Internal-Token", token)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment Service lỗi: " + e.getStatusCode());
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment Service không phản hồi");
        }
    }
}
