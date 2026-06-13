package com.malafi.payments.malafi_payments.psp.dto;

public enum PaymentProviderFailureCode {
    PSP_ERROR(true),
    TIMEOUT(true),
    INSUFFICIENT_FUNDS(false),
    INVALID_CARD(false);

    private final boolean retryable;

    PaymentProviderFailureCode(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
