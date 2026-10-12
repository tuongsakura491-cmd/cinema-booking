package com.cinema.booking.service;

import com.cinema.booking.client.HoldResult;
import com.cinema.booking.client.PaymentClient;
import com.cinema.booking.client.PaymentResult;
import com.cinema.booking.client.ShowtimeClient;
import com.cinema.booking.dto.BookingResponse;
import com.cinema.booking.dto.CreateBookingRequest;
import com.cinema.booking.dto.PayInfo;
import com.cinema.booking.dto.SeatItem;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.entity.BookingStatus;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepo;
    private final BookingSeatRepository seatRepo;
    private final ShowtimeClient showtimeClient;
    private final PaymentClient paymentClient;
    private final long cancelMinHoursBefore;

    public BookingService(BookingRepository bookingRepo, BookingSeatRepository seatRepo,
                          ShowtimeClient showtimeClient, PaymentClient paymentClient,
                          @Value("${booking.cancel-min-hours-before}") long cancelMinHoursBefore) {
        this.bookingRepo = bookingRepo;
        this.seatRepo = seatRepo;
        this.showtimeClient = showtimeClient;
        this.paymentClient = paymentClient;
        this.cancelMinHoursBefore = cancelMinHoursBefore;
    }

    // ================= TẠO BOOKING (giữ nguyên) =================
    public BookingResponse create(Long userId, CreateBookingRequest req) {
        List<Long> seatIds = req.seatIds().stream().distinct().toList();

        // Bước 1: lưu booking PENDING (commit ngay)
        Booking b = new Booking();
        b.setUserId(userId);
        b.setShowtimeId(req.showtimeId());
        b.setStatus(BookingStatus.PENDING);
        b = bookingRepo.save(b);

        // Bước 2: gọi Showtime giữ ghế
        HoldResult hold;
        try {
            hold = showtimeClient.hold(req.showtimeId(), b.getId(), userId, seatIds);
        } catch (ResponseStatusException e) {
            markFailed(b);
            // Timeout/mất kết nối: không biết Showtime đã giữ ghế chưa => bù trừ bằng release
            if (e.getStatusCode().value() == 503) {
                safeRelease(b.getId());
            }
            throw e;
        }

        // Bước 3: lưu snapshot. Nếu lỗi => compensation: nhả ghế
        try {
            b.setMovieTitle(hold.movieTitle());
            b.setCinemaName(hold.cinemaName());
            b.setRoomName(hold.roomName());
            b.setStartTime(hold.startTime());
            b.setTotalPrice(hold.totalPrice());
            b.setHoldExpiresAt(hold.expiresAt());
            b = bookingRepo.save(b);

            List<BookingSeat> rows = new ArrayList<>();
            for (SeatItem s : hold.seats()) {
                BookingSeat bs = new BookingSeat();
                bs.setBookingId(b.getId());
                bs.setSeatId(s.seatId());
                bs.setLabel(s.label());
                bs.setSeatType(s.type());
                bs.setPrice(s.price());
                rows.add(bs);
            }
            seatRepo.saveAll(rows);
        } catch (RuntimeException e) {
            log.error("Lưu booking {} lỗi sau khi hold, đang nhả ghế", b.getId(), e);
            safeRelease(b.getId());
            markFailed(b);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo booking, vui lòng thử lại");
        }
        return toResponse(b);
    }

    // ================= XÁC NHẬN = THANH TOÁN + CHỐT GHẾ =================
    public BookingResponse confirm(Long userId, Long bookingId, PayInfo pay) {
        Booking b = findOwned(userId, bookingId);
        expireIfNeeded(b);

        if (b.getStatus() == BookingStatus.CONFIRMED) {
            return toResponse(b); // gọi lặp lại: idempotent
        }
        if (b.getStatus() == BookingStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.GONE, "Booking đã hết hạn giữ ghế");
        }
        if (b.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking ở trạng thái " + b.getStatus() + ", không thể xác nhận");
        }
        if (b.getTotalPrice() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking chưa có thông tin giá");
        }

        // Bước 1: thanh toán. Số tiền lấy từ booking_db, KHÔNG nhận từ client.
        // Payment idempotent theo bookingId nên gọi lại sau lỗi mạng không trừ tiền hai lần.
        PaymentResult pr = paymentClient.pay(b.getId(), userId, b.getTotalPrice(), pay);
        if ("REFUNDED".equals(pr.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch của booking này đã được hoàn tiền");
        }
        if (!"SUCCESS".equals(pr.status())) {
            // Thanh toán thất bại: booking vẫn PENDING, còn thời gian giữ ghế thì người dùng thử lại được
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    pr.message() != null ? pr.message() : "Thanh toán không thành công");
        }
        if (!b.isPaid()) {
            bookingRepo.markPaid(b.getId());
            b.setPaid(true);
        }

        // Bước 2: chốt ghế. Nếu lỗi sau khi đã thu tiền thì phải bù trừ.
        try {
            showtimeClient.confirm(b.getId());
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == 410) {
                // Đã trả tiền nhưng ghế hết hạn/mất => hoàn tiền rồi đóng booking
                if (tryRefund(b)) {
                    b.setPaid(false);
                    b.setStatus(BookingStatus.EXPIRED);
                    bookingRepo.save(b);
                }
            }
            // 503/502: giữ nguyên PENDING + paid. Người dùng bấm lại: pay trả SUCCESS cũ (không trừ thêm),
            // confirm ghế được gọi lại (idempotent). Nếu quá hạn thì expireIfNeeded sẽ hoàn tiền.
            throw e;
        }
        b.setStatus(BookingStatus.CONFIRMED);
        b.setHoldExpiresAt(null);
        return toResponse(bookingRepo.save(b));
    }

    // ================= HỦY =================
    public BookingResponse cancel(Long userId, Long bookingId) {
        Booking b = findOwned(userId, bookingId);
        expireIfNeeded(b);

        if (b.getStatus() == BookingStatus.CANCELLED) {
            return toResponse(b);
        }
        if (b.getStatus() != BookingStatus.PENDING && b.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking ở trạng thái " + b.getStatus() + ", không thể hủy");
        }
        if (b.getStatus() == BookingStatus.CONFIRMED && b.getStartTime() != null
                && LocalDateTime.now().plusHours(cancelMinHoursBefore).isAfter(b.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ được hủy vé trước giờ chiếu " + cancelMinHoursBefore + " tiếng");
        }

        // Thứ tự an toàn: HOÀN TIỀN trước, rồi mới nhả ghế. Cả hai bước đều idempotent.
        // Lỗi ở bước nào cũng ném exception, booking giữ nguyên trạng thái, người dùng bấm hủy lại được.
        if (b.isPaid()) {
            paymentClient.refund(b.getId());
            b.setPaid(false);
        }
        showtimeClient.release(b.getId());
        b.setStatus(BookingStatus.CANCELLED);
        b.setHoldExpiresAt(null);
        return toResponse(bookingRepo.save(b));
    }

    // ================= TRUY VẤN =================
    public BookingResponse get(Long userId, Long bookingId) {
        Booking b = findOwned(userId, bookingId);
        expireIfNeeded(b);
        return toResponse(b);
    }

    public List<BookingResponse> listMine(Long userId) {
        List<Booking> list = bookingRepo.findByUserIdOrderByCreatedAtDesc(userId);
        list.forEach(this::expireIfNeeded);
        return list.stream().map(this::toResponse).toList();
    }

    // ================= HẾT HẠN =================
    /** Job dọn: chuyển PENDING quá hạn thành EXPIRED và nhả ghế (đã bật @Scheduled). */
    @Scheduled(fixedDelay = 30_000, initialDelay = 20_000)
    public void expireOverdue() {
        List<Booking> overdue = bookingRepo.findByStatusAndHoldExpiresAtBefore(BookingStatus.PENDING, LocalDateTime.now());
        overdue.forEach(this::expireIfNeeded);
    }

    private void expireIfNeeded(Booking b) {
        if (b.getStatus() == BookingStatus.PENDING && b.getHoldExpiresAt() != null
                && b.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            // Nếu đã thu tiền mà không chốt được ghế thì phải hoàn tiền trước khi đóng booking.
            // Hoàn chưa được => giữ PENDING, job 30 giây sau thử lại (không để mất tiền người dùng).
            if (b.isPaid()) {
                if (!tryRefund(b)) return;
                b.setPaid(false);
            }
            b.setStatus(BookingStatus.EXPIRED);
            bookingRepo.save(b);
            safeRelease(b.getId());
        }
    }

    // ================= helper =================
    private Booking findOwned(Long userId, Long bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy booking"));
        if (!b.getUserId().equals(userId)) {
            // Trả 404 thay vì 403 để không lộ sự tồn tại của booking người khác
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy booking");
        }
        return b;
    }

    private boolean tryRefund(Booking b) {
        try {
            paymentClient.refund(b.getId());
            return true;
        } catch (RuntimeException e) {
            log.error("Hoàn tiền booking {} thất bại, sẽ thử lại: {}", b.getId(), e.getMessage());
            return false;
        }
    }

    private void markFailed(Booking b) {
        try {
            b.setStatus(BookingStatus.FAILED);
            bookingRepo.save(b);
        } catch (RuntimeException e) {
            log.error("Không đánh dấu FAILED được cho booking {}", b.getId(), e);
        }
    }

    private void safeRelease(Long bookingId) {
        try {
            showtimeClient.release(bookingId);
        } catch (RuntimeException e) {
            // Không làm hỏng luồng chính: ghế quá hạn vẫn tự được coi là trống
            log.warn("Release booking {} thất bại: {}", bookingId, e.getMessage());
        }
    }

    private BookingResponse toResponse(Booking b) {
        List<SeatItem> seats = seatRepo.findByBookingId(b.getId()).stream()
                .map(s -> new SeatItem(s.getSeatId(), s.getLabel(), s.getSeatType(), s.getPrice()))
                .toList();
        return new BookingResponse(b.getId(), b.getShowtimeId(), b.getMovieTitle(), b.getCinemaName(),
                b.getRoomName(), b.getStartTime(), seats, b.getTotalPrice(),
                b.getStatus().name(), b.getHoldExpiresAt(), b.getCreatedAt());
    }
}
