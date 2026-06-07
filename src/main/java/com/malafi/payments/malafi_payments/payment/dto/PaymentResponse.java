package com.malafi.payments.malafi_payments.payment.dto;

import com.malafi.payments.malafi_payments.payment.Currency;
import com.malafi.payments.malafi_payments.payment.Payment;
import com.malafi.payments.malafi_payments.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long paymentId,
        Long merchantId,
        BigDecimal amount,
        Currency currency,
        PaymentStatus status,
        Instant createdAt,
        Instant modifiedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getMerchant().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getModifiedAt()
        );
    }
}
