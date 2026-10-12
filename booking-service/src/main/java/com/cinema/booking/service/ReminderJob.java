package com.cinema.booking.service;

import com.cinema.booking.client.NotificationClient;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.entity.BookingStatus;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Mỗi phút quét các vé CONFIRMED sắp chiếu (trong vòng reminder.minutes-before phút)
 * mà chưa nhắc, rồi nhờ Notification Service tạo thông báo.
 * Dữ liệu phim/rạp/giờ lấy từ snapshot trong booking_db => không gọi chéo service để đọc.
 */
@Component
public class ReminderJob {
    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM");

    private final BookingRepository bookingRepo;
    private final BookingSeatRepository seatRepo;
    private final NotificationClient notificationClient;
    private final long minutesBefore;

    public ReminderJob(BookingRepository bookingRepo, BookingSeatRepository seatRepo,
                       NotificationClient notificationClient,
                       @Value("${reminder.minutes-before:60}") long minutesBefore) {
        this.bookingRepo = bookingRepo;
        this.seatRepo = seatRepo;
        this.notificationClient = notificationClient;
        this.minutesBefore = minutesBefore;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> due = bookingRepo.findByStatusAndReminderSentFalseAndStartTimeBetween(
                BookingStatus.CONFIRMED, now, now.plusMinutes(minutesBefore));

        for (Booking b : due) {
            try {
                List<String> seats = seatRepo.findByBookingId(b.getId()).stream()
                        .map(BookingSeat::getLabel).toList();
                long mins = Math.max(0, Duration.between(now, b.getStartTime()).toMinutes());
                String content = String.format("Phim \"%s\" bắt đầu lúc %s (còn khoảng %d phút) tại %s - %s. Ghế: %s.",
                        b.getMovieTitle(), b.getStartTime().format(TIME), mins,
                        b.getCinemaName(), b.getRoomName(), String.join(", ", seats));

                // dedupeKey: nếu gửi rồi mà chưa kịp đánh dấu, lần sau gửi lại cũng không bị trùng
                notificationClient.send(b.getUserId(), "REMINDER", "Sắp đến giờ chiếu", content,
                        "REMINDER:" + b.getId(), b.getStartTime());

                // Update đúng 1 cột, không save cả entity (tránh ghi đè status nếu user vừa hủy vé)
                bookingRepo.markReminderSent(b.getId());
            } catch (RuntimeException e) {
                // Notification lỗi KHÔNG ảnh hưởng đặt vé; phút sau job tự thử lại
                log.warn("Gửi nhắc giờ chiếu cho booking {} thất bại: {}", b.getId(), e.getMessage());
            }
        }
    }
}
