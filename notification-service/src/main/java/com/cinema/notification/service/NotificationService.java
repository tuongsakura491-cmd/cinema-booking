package com.cinema.notification.service;

import com.cinema.notification.dto.CreateNotificationRequest;
import com.cinema.notification.dto.NotificationResponse;
import com.cinema.notification.entity.Notification;
import com.cinema.notification.repository.NotificationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    /**
     * Idempotent: cùng dedupeKey thì trả lại thông báo cũ, không tạo bản thứ hai.
     * Cố ý KHÔNG dùng @Transactional ở đây: nếu hai request trùng nhau đụng UNIQUE,
     * ta bắt lỗi rồi đọc lại bản đã có trong một transaction mới.
     */
    public NotificationResponse create(CreateNotificationRequest r) {
        String key = r.dedupeKey();
        if (key != null && !key.isBlank()) {
            var existing = repo.findByDedupeKey(key);
            if (existing.isPresent()) return toDto(existing.get());
        }

        Notification n = new Notification();
        n.setUserId(r.userId());
        n.setType(r.type());
        n.setTitle(r.title());
        n.setContent(r.content());
        n.setDedupeKey(key == null || key.isBlank() ? null : key);
        if (r.expiresAt() != null && !r.expiresAt().isBlank()) {
            try {
                n.setExpiresAt(LocalDateTime.parse(r.expiresAt()));
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expiresAt phải theo dạng ISO-8601");
            }
        }
        try {
            return toDto(repo.saveAndFlush(n));
        } catch (DataIntegrityViolationException e) {
            return repo.findByDedupeKey(n.getDedupeKey()).map(this::toDto).orElseThrow(() -> e);
        }
    }

    public List<NotificationResponse> listMine(Long userId) {
        return repo.findVisible(userId, LocalDateTime.now(), PageRequest.of(0, 50))
                .stream().map(this::toDto).toList();
    }

    public long unreadCount(Long userId) {
        return repo.countUnread(userId, LocalDateTime.now());
    }

    public NotificationResponse markRead(Long userId, Long id) {
        Notification n = repo.findById(id)
                .filter(x -> x.getUserId().equals(userId))   // không phải của mình thì coi như không tồn tại
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"));
        n.setSeen(true);
        return toDto(repo.save(n));
    }

    public int markAllRead(Long userId) {
        return repo.markAllRead(userId);
    }

    private NotificationResponse toDto(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getContent(),
                n.isSeen(), n.getCreatedAt());
    }
}
