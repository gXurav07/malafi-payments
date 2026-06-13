package com.malafi.payments.malafi_payments.psp;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentProviderRegistry {

    private final Map<PspName, PaymentProviderAdapter> adapters;

    public PaymentProviderRegistry(List<PaymentProviderAdapter> adapters) {
        this.adapters = new EnumMap<>(PspName.class);
        for (PaymentProviderAdapter adapter : adapters) {
            this.adapters.put(adapter.providerName(), adapter);
        }
    }

    public PaymentProviderAdapter get(PspName pspName) {
        PaymentProviderAdapter adapter = adapters.get(pspName);
        if (adapter == null) {
            throw new IllegalStateException("Payment provider adapter not found for " + pspName);
        }
        return adapter;
    }
}
