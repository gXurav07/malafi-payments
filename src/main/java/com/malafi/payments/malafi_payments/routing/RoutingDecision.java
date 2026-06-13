package com.malafi.payments.malafi_payments.routing;

import com.malafi.payments.malafi_payments.common.BaseEntity;
import com.malafi.payments.malafi_payments.payment.Payment;
import com.malafi.payments.malafi_payments.psp.PspName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "routing_decisions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutingDecision extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RoutingStrategy strategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PspName selectedPsp;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal selectedScore;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String candidateSummary;

    public RoutingDecision(Payment payment, RoutingResult routingResult) {
        this.payment = payment;
        this.strategy = routingResult.strategy();
        this.selectedPsp = routingResult.selectedPsp();
        this.selectedScore = routingResult.selectedScore();
        this.reason = routingResult.reason();
        this.candidateSummary = routingResult.candidateSummary();
    }
}
