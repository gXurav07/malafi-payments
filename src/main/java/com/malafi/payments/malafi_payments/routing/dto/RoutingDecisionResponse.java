package com.malafi.payments.malafi_payments.routing.dto;

import com.malafi.payments.malafi_payments.psp.PspName;
import com.malafi.payments.malafi_payments.routing.RoutingDecision;
import com.malafi.payments.malafi_payments.routing.RoutingStrategy;

import java.math.BigDecimal;
import java.time.Instant;

public record RoutingDecisionResponse(
        Long routingDecisionId,
        Long paymentId,
        RoutingStrategy strategy,
        PspName selectedPsp,
        BigDecimal selectedScore,
        String reason,
        String candidateSummary,
        Instant createdAt,
        Instant modifiedAt
) {
    public static RoutingDecisionResponse from(RoutingDecision routingDecision) {
        return new RoutingDecisionResponse(
                routingDecision.getId(),
                routingDecision.getPayment().getId(),
                routingDecision.getStrategy(),
                routingDecision.getSelectedPsp(),
                routingDecision.getSelectedScore(),
                routingDecision.getReason(),
                routingDecision.getCandidateSummary(),
                routingDecision.getCreatedAt(),
                routingDecision.getModifiedAt()
        );
    }
}
