package com.malafi.payments.malafi_payments.psp.mock;

import com.malafi.payments.malafi_payments.psp.PaymentProviderAdapter;
import com.malafi.payments.malafi_payments.psp.PspName;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderRequest;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class CashfreeMockAdapter extends PaymentProviderAdapter {

    private static final int EXTRA_NORMAL_DELAY_MS = 350;
    private static final int EXTRA_TIMEOUT_DELAY_MS = 500;

    @Override
    public PspName providerName() {
        return PspName.CASHFREE_MOCK;
    }

    @Override
    public PaymentProviderResult process(PaymentProviderRequest request) {
        int outcome = ThreadLocalRandom.current().nextInt(100);
        int delay = sleepRandomDelay(MIN_NORMAL_DELAY_MS, EXTRA_NORMAL_DELAY_MS);

        if (outcome < 80) {
            return PaymentProviderResult.success("cf_mock_" + UUID.randomUUID().toString().replace("-", ""));
        }
        if (outcome < 90) {
            return PaymentProviderResult.failed("Cashfree mock payment failed");
        }
        sleepExactDelay(MIN_TIMEOUT_DELAY_MS + EXTRA_TIMEOUT_DELAY_MS - delay);
        return PaymentProviderResult.timeout("Cashfree mock timeout");
    }


}
