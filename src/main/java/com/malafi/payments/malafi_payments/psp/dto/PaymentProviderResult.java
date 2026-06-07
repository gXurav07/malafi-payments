package com.malafi.payments.malafi_payments.psp.dto;

public record PaymentProviderResult(
        PaymentProviderStatus status,
        String providerReferenceId,
        String failureReason
) {
    public static PaymentProviderResult success(String providerReferenceId) {
        return new PaymentProviderResult(PaymentProviderStatus.SUCCESS, providerReferenceId, null);
    }

    public static PaymentProviderResult failed(String failureReason) {
        return new PaymentProviderResult(PaymentProviderStatus.FAILED, null, failureReason);
    }

    public static PaymentProviderResult timeout(String failureReason) {
        return new PaymentProviderResult(PaymentProviderStatus.TIMEOUT, null, failureReason);
    }
}
