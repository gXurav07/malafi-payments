package com.malafi.payments.malafi_payments.paymentattempt;

import com.malafi.payments.malafi_payments.common.BaseEntity;
import com.malafi.payments.malafi_payments.payment.Payment;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderFailureCode;
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
@Table(name = "payment_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false, length = 60)
    private String pspName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentAttemptStatus status;

    @Column(length = 500)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private PaymentProviderFailureCode failureCode;

    @Column(length = 120)
    private String providerReferenceId;

    @Column
    private Long latencyMs;

    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    public PaymentAttempt(Payment payment, String pspName) {
        this.payment = payment;
        this.pspName = pspName;
        this.status = PaymentAttemptStatus.INITIATED;
    }

    public void markSuccess(String providerReferenceId, PaymentProviderFailureCode failureCode, Long latencyMs, BigDecimal cost) {
        this.status = PaymentAttemptStatus.SUCCESS;
        this.providerReferenceId = providerReferenceId;
        this.failureReason = null;
        this.failureCode = failureCode;
        recordMetrics(latencyMs, cost);
    }

    public void markFailed(PaymentProviderFailureCode failureCode, String failureReason, Long latencyMs, BigDecimal cost) {
        this.status = PaymentAttemptStatus.FAILED;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        recordMetrics(latencyMs, cost);
    }

    public void markTimeout(PaymentProviderFailureCode failureCode, String failureReason, Long latencyMs, BigDecimal cost) {
        this.status = PaymentAttemptStatus.TIMEOUT;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        recordMetrics(latencyMs, cost);
    }

    private void recordMetrics(Long latencyMs, BigDecimal cost) {
        this.latencyMs = latencyMs;
        this.cost = cost;
    }
}
