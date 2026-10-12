package com.cinema.booking.service;

import com.cinema.booking.client.NotificationClient;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.entity.BookingStatus;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReminderJob {
    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM");
    private static final int[] REMINDER_MINUTES = {60, 30, 15, 0};
    private static final String[] TITLES = {
            "Phim sắp bắt đầu sau 60 phút",
            "Phim sắp bắt đầu sau 30 phút",
            "Phim sắp bắt đầu sau 15 phút",
            "Chúc bạn xem phim vui vẻ!"
    };

    private final BookingRepository bookingRepo;
    private final BookingSeatRepository seatRepo;
    private final NotificationClient notificationClient;

    public ReminderJob(BookingRepository bookingRepo, BookingSeatRepository seatRepo,
                       NotificationClient notificationClient) {
        this.bookingRepo = bookingRepo;
        this.seatRepo = seatRepo;
        this.notificationClient = notificationClient;
    }

    @Scheduled(fixedDelay = 10_000, initialDelay = 15_000)
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> due = bookingRepo.findByStatusAndStartTimeLessThanEqualAndReminderStageLessThan(
                BookingStatus.CONFIRMED, now.plusMinutes(60), REMINDER_MINUTES.length);

        for (Booking booking : due) {
            sendDueReminders(booking, now);
        }
    }

    private void sendDueReminders(Booking booking, LocalDateTime now) {
        if (booking.getStartTime() == null) return;

        List<String> seats = seatRepo.findByBookingId(booking.getId()).stream()
                .map(BookingSeat::getLabel).toList();
        String details = String.format("Phim \"%s\" bắt đầu lúc %s tại %s - %s. Ghế: %s.",
                booking.getMovieTitle(), booking.getStartTime().format(TIME),
                booking.getCinemaName(), booking.getRoomName(), String.join(", ", seats));

        // Dữ liệu từ phiên bản cũ chỉ có reminderSent; xem như mốc 60 phút đã gửi.
        int stage = booking.getReminderStage();
        if (stage == 0 && booking.isReminderSent()) {
            bookingRepo.migrateLegacyReminder(booking.getId());
            stage = 1;
        }
        while (stage < REMINDER_MINUTES.length) {
            int minutes = REMINDER_MINUTES[stage];
            if (booking.getStartTime().isAfter(now.plusMinutes(minutes))) break;

            try {
                String content = minutes == 0
                        ? "Chúc bạn xem phim vui vẻ! " + details
                        : details + String.format(" Còn khoảng %d phút nữa là đến giờ chiếu.", minutes);
                notificationClient.send(booking.getUserId(), "REMINDER", TITLES[stage], content,
                        "REMINDER:" + booking.getId() + ":" + minutes,
                        minutes == 0 ? null : booking.getStartTime());

                int updated = bookingRepo.advanceReminderStage(booking.getId(), stage, stage + 1);
                if (updated == 0) break;
                stage++;
            } catch (RuntimeException e) {
                log.warn("Gửi nhắc giờ chiếu cho booking {} (mốc {} phút) thất bại: {}",
                        booking.getId(), minutes, e.getMessage());
                break;
            }
        }
    }
}
