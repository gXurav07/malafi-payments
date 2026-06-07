package com.malafi.payments.malafi_payments.paymentattempt;

import com.malafi.payments.malafi_payments.common.BaseEntity;
import com.malafi.payments.malafi_payments.payment.Payment;
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

    @Column(length = 120)
    private String providerReferenceId;

    public PaymentAttempt(Payment payment, String pspName) {
        this.payment = payment;
        this.pspName = pspName;
        this.status = PaymentAttemptStatus.INITIATED;
    }

    public void markSuccess(String providerReferenceId) {
        this.status = PaymentAttemptStatus.SUCCESS;
        this.providerReferenceId = providerReferenceId;
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = PaymentAttemptStatus.FAILED;
        this.failureReason = failureReason;
    }

    public void markTimeout(String failureReason) {
        this.status = PaymentAttemptStatus.TIMEOUT;
        this.failureReason = failureReason;
    }
}
