package com.cinema.booking.dto;

import jakarta.validation.constraints.Pattern;

/** Thông tin thanh toán client gửi kèm khi xác nhận. KHÔNG có số tiền: tiền do server tính. */
public record PayInfo(
        String method,                                    // CARD | EWALLET | BANK_TRANSFER
        @Pattern(regexp = "\\d{4}") String cardLast4) {   // 4 số cuối thẻ (mô phỏng); 0000 = bị từ chối
}
