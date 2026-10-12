package com.cinema.payment.service;

import com.cinema.payment.dto.PayRequest;
import com.cinema.payment.dto.PayResult;
import com.cinema.payment.dto.PaymentResponse;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentStatus;
import com.cinema.payment.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Set<String> METHODS = Set.of("CARD", "EWALLET", "BANK_TRANSFER");
    private static final String FAIL_CARD = "0000";   // thẻ thử luôn bị từ chối (mô phỏng)

    private final PaymentRepository repo;
    private final TransactionTemplate tx;

    public PaymentService(PaymentRepository repo, TransactionTemplate tx) {
        this.repo = repo;
        this.tx = tx;
    }

    /**
     * Idempotent theo bookingId:
     *  - chưa có  -> tạo mới rồi xử lý
     *  - FAILED   -> cho thử lại
     *  - SUCCESS  -> trả lại kết quả cũ, KHÔNG trừ tiền lần hai
     */
    public PayResult pay(PayRequest r) {
        String method = (r.method() == null || r.method().isBlank()) ? "CARD" : r.method().toUpperCase();
        if (!METHODS.contains(method)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phương thức thanh toán không hỗ trợ");
        }
        Payment p = getOrCreate(r, method);
        return tx.execute(s -> process(p.getId(), r, method));
    }

    /** Không @Transactional: nếu hai request tạo cùng lúc đụng UNIQUE, ta đọc lại bản đã có. */
    private Payment getOrCreate(PayRequest r, String method) {
        return repo.findByBookingId(r.bookingId()).orElseGet(() -> {
            Payment n = new Payment();
            n.setBookingId(r.bookingId());
            n.setUserId(r.userId());
            n.setAmount(r.amount());
            n.setMethod(method);
            try {
                return repo.saveAndFlush(n);
            } catch (DataIntegrityViolationException e) {
                return repo.findByBookingId(r.bookingId()).orElseThrow(() -> e);
            }
        });
    }

    private PayResult process(Long id, PayRequest r, String method) {
        Payment p = repo.findByIdForUpdate(id).orElseThrow();   // khóa dòng
        if (!p.getUserId().equals(r.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Giao dịch không thuộc người dùng này");
        }
        switch (p.getStatus()) {
            case SUCCESS:
                return result(p, "Đã thanh toán trước đó");
            case REFUNDED:
                return result(p, "Giao dịch đã được hoàn tiền");
            default:
                break;   // PENDING hoặc FAILED: xử lý (lại)
        }

        p.setMethod(method);
        if (FAIL_CARD.equals(r.cardLast4())) {
            p.setStatus(PaymentStatus.FAILED);
            p.setFailureReason("Thẻ bị từ chối (mô phỏng)");
        } else {
            p.setStatus(PaymentStatus.SUCCESS);
            p.setFailureReason(null);
            p.setTransactionCode("TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        }
        p.setUpdatedAt(LocalDateTime.now());
        repo.save(p);
        return result(p, p.getStatus() == PaymentStatus.SUCCESS ? "Thanh toán thành công" : p.getFailureReason());
    }

    /**
     * Hoàn tiền, idempotent: gọi nhiều lần vẫn ổn.
     * Không có giao dịch / chưa trả tiền -> không làm gì (NONE / FAILED / PENDING).
     */
    @Transactional
    public String refund(Long bookingId) {
        var opt = repo.findByBookingIdForUpdate(bookingId);
        if (opt.isEmpty()) return "NONE";
        Payment p = opt.get();
        if (p.getStatus() == PaymentStatus.SUCCESS) {
            p.setStatus(PaymentStatus.REFUNDED);
            p.setUpdatedAt(LocalDateTime.now());
            repo.save(p);
        }
        return p.getStatus().name();
    }

    public List<PaymentResponse> listMine(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(p -> new PaymentResponse(p.getId(), p.getBookingId(), p.getAmount(), p.getMethod(),
                        p.getStatus().name(), p.getTransactionCode(), p.getCreatedAt()))
                .toList();
    }

    private PayResult result(Payment p, String message) {
        return new PayResult(p.getId(), p.getStatus().name(), message, p.getTransactionCode());
    }
}
