package com.cinema.booking.repository;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// BẢN GỘP: gồm cả phần nhắc giờ chiếu (gói notification) và phần thanh toán. Thay thế bản cũ.
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByStatusAndHoldExpiresAtBefore(BookingStatus status, LocalDateTime time);

    // Các booking còn mốc nhắc chưa gửi và đã vào cửa sổ 60 phút trước giờ chiếu.
    List<Booking> findByStatusAndStartTimeLessThanEqualAndReminderStageLessThan(
            BookingStatus status, LocalDateTime to, int stage);

    @Transactional
    @Modifying
    @Query("update Booking b set b.reminderStage = :stage, b.reminderSent = true " +
            "where b.id = :id and b.reminderStage = :previousStage")
    int advanceReminderStage(@Param("id") Long id, @Param("previousStage") int previousStage,
                             @Param("stage") int stage);

    @Transactional
    @Modifying
    @Query("update Booking b set b.reminderStage = 1 where b.id = :id " +
            "and b.reminderStage = 0 and b.reminderSent = true")
    int migrateLegacyReminder(@Param("id") Long id);

    // Thanh toán: đánh dấu đã thu tiền (chỉ đụng 1 cột)
    @Transactional
    @Modifying
    @Query("update Booking b set b.paid = true where b.id = :id")
    int markPaid(@Param("id") Long id);
}
