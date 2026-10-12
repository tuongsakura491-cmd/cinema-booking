package com.cinema.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PayRequest(
        @NotNull Long bookingId,
        @NotNull Long userId,
        @NotNull @Positive Long amount,
        String method,                                   // CARD | EWALLET | BANK_TRANSFER (mặc định CARD)
        @Pattern(regexp = "\\d{4}") String cardLast4) {  // CHỈ 4 số cuối để mô phỏng, không lưu số thẻ
}
