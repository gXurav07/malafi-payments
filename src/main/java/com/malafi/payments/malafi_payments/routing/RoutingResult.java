package com.malafi.payments.malafi_payments.routing;

import com.malafi.payments.malafi_payments.psp.PspName;

import java.math.BigDecimal;
import java.util.List;

public record RoutingResult(
        RoutingStrategy strategy,
        PspName selectedPsp,
        BigDecimal selectedScore,
        String reason,
        String candidateSummary,
        List<PspName> orderedCandidates
) {
}
