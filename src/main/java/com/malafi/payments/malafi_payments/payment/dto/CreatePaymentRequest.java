package com.malafi.payments.malafi_payments.payment.dto;

import com.malafi.payments.malafi_payments.payment.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull(message = "Merchant id is required")
        Long merchantId,

        @NotNull(message = "Payment amount is required")
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "Payment amount can have at most 17 integer digits and 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "Currency is required")
        Currency currency
) {
}
