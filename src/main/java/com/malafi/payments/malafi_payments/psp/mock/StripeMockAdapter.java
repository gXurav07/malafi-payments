package com.malafi.payments.malafi_payments.psp.mock;

import com.malafi.payments.malafi_payments.psp.PaymentProviderAdapter;
import com.malafi.payments.malafi_payments.psp.PspName;
import com.malafi.payments.malafi_payments.psp.PspProperties;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderRequest;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StripeMockAdapter extends PaymentProviderAdapter {

    private final PspProperties pspProperties;

    @Override
    public PspName providerName() {
        return PspName.STRIPE_MOCK;
    }

    @Override
    public PaymentProviderResult process(PaymentProviderRequest request) {
        return simulate(
                pspProperties.provider(providerName()),
                "stripe_mock_",
                "Stripe mock payment failed",
                "Stripe mock timeout"
        );
    }
}
