package com.malafi.payments.malafi_payments.psp.dto;

import com.malafi.payments.malafi_payments.payment.Currency;

import java.math.BigDecimal;

public record PaymentProviderRequest(
        Long attemptId,
        Long paymentId,
        Long merchantId,
        BigDecimal amount,
        Currency currency
) {
}
