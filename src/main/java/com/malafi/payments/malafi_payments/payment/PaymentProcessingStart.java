package com.malafi.payments.malafi_payments.payment;

import com.malafi.payments.malafi_payments.psp.PspName;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderRequest;

import java.util.List;

public record PaymentProcessingStart(
        PspName selectedPsp,
        PaymentProviderRequest providerRequest,
        List<PspName> remainingPsps
) {
}
