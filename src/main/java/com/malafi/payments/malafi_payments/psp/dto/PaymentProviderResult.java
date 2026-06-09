package com.malafi.payments.malafi_payments.psp.dto;

import java.math.BigDecimal;

public record PaymentProviderResult(
        PaymentProviderStatus status,
        String providerReferenceId,
        String failureReason,
        Long latencyMs,
        BigDecimal cost
) {
    public static PaymentProviderResult success(String providerReferenceId) {
        return new PaymentProviderResult(PaymentProviderStatus.SUCCESS, providerReferenceId, null, null, null);
    }

    public static PaymentProviderResult failed(String failureReason) {
        return new PaymentProviderResult(PaymentProviderStatus.FAILED, null, failureReason, null, null);
    }

    public static PaymentProviderResult timeout(String failureReason) {
        return new PaymentProviderResult(PaymentProviderStatus.TIMEOUT, null, failureReason, null, null);
    }

    public PaymentProviderResult withMetrics(long latencyMs, BigDecimal cost) {
        return new PaymentProviderResult(status, providerReferenceId, failureReason, latencyMs, cost);
    }
}
