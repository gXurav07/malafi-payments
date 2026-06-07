package com.malafi.payments.malafi_payments.psp;

import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderRequest;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderResult;

import java.util.concurrent.ThreadLocalRandom;

public abstract class PaymentProviderAdapter {
    protected static final int MIN_NORMAL_DELAY_MS = 80;
    protected static final int MIN_TIMEOUT_DELAY_MS = 1_000;

    abstract public PspName providerName();

    abstract public PaymentProviderResult process(PaymentProviderRequest request);


    protected int sleepRandomDelay(int minDelayMs, int extraDelayMs) {
        int maxDelayMs = minDelayMs + extraDelayMs;
        int delayMs = ThreadLocalRandom.current().nextInt(minDelayMs, maxDelayMs + 1);
        sleepExactDelay(delayMs);
        return delayMs;
    }

    protected void sleepExactDelay(int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
