package com.malafi.payments.malafi_payments.psp;

import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderRequest;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderFailureCode;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderResult;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public abstract class PaymentProviderAdapter {

    abstract public PspName providerName();

    abstract public PaymentProviderResult process(PaymentProviderRequest request);

    protected PaymentProviderResult simulate(
            PspProperties.Provider provider,
            String providerReferencePrefix,
            String failureReason,
            String timeoutReason) {
        if (!provider.isEnabled()) {
            return PaymentProviderResult.failed(providerName() + " is disabled");
        }

        int totalRate = provider.getSuccessRate() + provider.getFailureRate() + provider.getTimeoutRate();
        if (totalRate <= 0) {
            return PaymentProviderResult.failed(providerName() + " has invalid outcome configuration");
        }

        int outcome = ThreadLocalRandom.current().nextInt(totalRate);
        if (outcome < provider.getSuccessRate()) {
            sleepRandomDelay(provider.getNormalDelayMinMs(), provider.getNormalDelayMaxMs());
            return PaymentProviderResult.success(providerReferencePrefix + UUID.randomUUID().toString().replace("-", ""));
        }

        if (outcome < provider.getSuccessRate() + provider.getFailureRate()) {
            sleepRandomDelay(provider.getNormalDelayMinMs(), provider.getNormalDelayMaxMs());
            return randomFailure(failureReason);
        }

        sleepRandomDelay(provider.getTimeoutDelayMinMs(), provider.getTimeoutDelayMaxMs());
        return PaymentProviderResult.timeout(timeoutReason);
    }

    private PaymentProviderResult randomFailure(String failureReason) {
        int outcome = ThreadLocalRandom.current().nextInt(100);
        if (outcome < 50) {
            return PaymentProviderResult.retryableFailure(PaymentProviderFailureCode.PSP_ERROR, failureReason);
        }
        if (outcome < 75) {
            return PaymentProviderResult.nonRetryableFailure(PaymentProviderFailureCode.INSUFFICIENT_FUNDS, "Insufficient funds");
        }
        return PaymentProviderResult.nonRetryableFailure(PaymentProviderFailureCode.INVALID_CARD, "Invalid card");
    }

    protected void sleepRandomDelay(int minDelayMs, int maxDelayMs) {
        int delayMs = ThreadLocalRandom.current().nextInt(minDelayMs, maxDelayMs + 1);
        sleepExactDelay(delayMs);
    }

    protected void sleepExactDelay(int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
