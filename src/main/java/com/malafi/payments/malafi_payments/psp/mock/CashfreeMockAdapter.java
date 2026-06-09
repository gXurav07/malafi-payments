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
public class CashfreeMockAdapter extends PaymentProviderAdapter {

    private final PspProperties pspProperties;

    @Override
    public PspName providerName() {
        return PspName.CASHFREE_MOCK;
    }

    @Override
    public PaymentProviderResult process(PaymentProviderRequest request) {
        return simulate(
                pspProperties.provider(providerName()),
                "cf_mock_",
                "Cashfree mock payment failed",
                "Cashfree mock timeout"
        );
    }
}
