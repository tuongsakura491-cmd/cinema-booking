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

    // Nhắc giờ chiếu
    List<Booking> findByStatusAndReminderSentFalseAndStartTimeBetween(
            BookingStatus status, LocalDateTime from, LocalDateTime to);

    @Transactional
    @Modifying
    @Query("update Booking b set b.reminderSent = true where b.id = :id")
    int markReminderSent(@Param("id") Long id);

    // Thanh toán: đánh dấu đã thu tiền (chỉ đụng 1 cột)
    @Transactional
    @Modifying
    @Query("update Booking b set b.paid = true where b.id = :id")
    int markPaid(@Param("id") Long id);
}
