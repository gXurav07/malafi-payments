package com.malafi.payments.malafi_payments.psp.dto;

import java.math.BigDecimal;

public record PaymentProviderResult(
        PaymentProviderStatus status,
        PaymentProviderFailureCode failureCode,
        String providerReferenceId,
        String failureReason,
        Long latencyMs,
        BigDecimal cost
) {
    public static PaymentProviderResult success(String providerReferenceId) {
        return new PaymentProviderResult(PaymentProviderStatus.SUCCESS, null, providerReferenceId, null, null, null);
    }

    public static PaymentProviderResult failed(String failureReason) {
        return retryableFailure(PaymentProviderFailureCode.PSP_ERROR, failureReason);
    }

    public static PaymentProviderResult retryableFailure(PaymentProviderFailureCode failureCode, String failureReason) {
        return new PaymentProviderResult(PaymentProviderStatus.FAILED, failureCode, null, failureReason, null, null);
    }

    public static PaymentProviderResult nonRetryableFailure(PaymentProviderFailureCode failureCode, String failureReason) {
        return new PaymentProviderResult(PaymentProviderStatus.FAILED, failureCode, null, failureReason, null, null);
    }

    public static PaymentProviderResult timeout(String failureReason) {
        return new PaymentProviderResult(PaymentProviderStatus.TIMEOUT, PaymentProviderFailureCode.TIMEOUT, null, failureReason, null, null);
    }

    public PaymentProviderResult withMetrics(long latencyMs, BigDecimal cost) {
        return new PaymentProviderResult(status, failureCode, providerReferenceId, failureReason, latencyMs, cost);
    }

    public boolean isRetryable() {
        return failureCode != null && failureCode.isRetryable();
    }
}
