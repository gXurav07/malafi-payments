package com.malafi.payments.malafi_payments.payment;

import com.malafi.payments.malafi_payments.common.BaseEntity;
import com.malafi.payments.malafi_payments.merchant.Merchant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public Payment(Merchant merchant, BigDecimal amount, Currency currency) {
        this.merchant = merchant;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.CREATED;
    }

    public void markProcessing() {
        requireStatus(PaymentStatus.CREATED, "Only CREATED payments can move to PROCESSING");
        this.status = PaymentStatus.PROCESSING;
    }

    public void markSuccess() {
        requireStatus(PaymentStatus.PROCESSING, "Only PROCESSING payments can move to SUCCESS");
        this.status = PaymentStatus.SUCCESS;
    }

    public void markFailed() {
        requireStatus(PaymentStatus.PROCESSING, "Only PROCESSING payments can move to FAILED");
        this.status = PaymentStatus.FAILED;
    }

    private void requireStatus(PaymentStatus expectedStatus, String message) {
        if (status != expectedStatus) {
            throw new IllegalStateException(message);
        }
    }
}
