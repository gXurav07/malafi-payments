package com.malafi.payments.malafi_payments.merchant;

import com.malafi.payments.malafi_payments.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "merchants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Merchant extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, updatable = false, length = 128)
    private String apiKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MerchantStatus status = MerchantStatus.ACTIVE;

    public Merchant(String name, String apiKey) {
        this.name = name;
        this.apiKey = apiKey;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void activate() {
        this.status = MerchantStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = MerchantStatus.INACTIVE;
    }
}
