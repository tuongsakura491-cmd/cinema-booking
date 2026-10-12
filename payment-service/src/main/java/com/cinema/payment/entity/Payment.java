package com.cinema.payment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Mỗi booking có tối đa MỘT payment (bookingId UNIQUE).
 * Thử thanh toán lại sau khi FAILED thì cập nhật lại chính dòng này,
 * còn đã SUCCESS thì gọi lại không bao giờ trừ tiền lần hai.
 */
@Entity
@Table(name = "payments", indexes = {@Index(name = "idx_payment_user", columnList = "userId")})
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long bookingId;      // tham chiếu logic sang booking_db, không có FK

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long amount;         // VND, do server (Booking) cung cấp, không nhận từ client

    @Column(nullable = false, length = 20)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    private String failureReason;
    private String transactionCode;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
