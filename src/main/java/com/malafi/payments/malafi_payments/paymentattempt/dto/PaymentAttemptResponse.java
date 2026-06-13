package com.malafi.payments.malafi_payments.paymentattempt.dto;

import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttempt;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttemptStatus;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderFailureCode;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentAttemptResponse(
        Long attemptId,
        Long paymentId,
        String pspName,
        PaymentAttemptStatus status,
        PaymentProviderFailureCode failureCode,
        String failureReason,
        String providerReferenceId,
        Long latencyMs,
        BigDecimal cost,
        Instant createdAt,
        Instant modifiedAt
) {
    public static PaymentAttemptResponse from(PaymentAttempt attempt) {
        return new PaymentAttemptResponse(
                attempt.getId(),
                attempt.getPayment().getId(),
                attempt.getPspName(),
                attempt.getStatus(),
                attempt.getFailureCode(),
                attempt.getFailureReason(),
                attempt.getProviderReferenceId(),
                attempt.getLatencyMs(),
                attempt.getCost(),
                attempt.getCreatedAt(),
                attempt.getModifiedAt()
        );
    }
}
