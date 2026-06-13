package com.malafi.payments.malafi_payments.routing;

import com.malafi.payments.malafi_payments.psp.PspName;

import java.math.BigDecimal;

public record RoutingCandidate(
        PspName pspName,
        BigDecimal score,
        String reason
) {
}
