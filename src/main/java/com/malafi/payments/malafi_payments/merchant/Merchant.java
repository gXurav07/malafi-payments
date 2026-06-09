package com.malafi.payments.malafi_payments.merchant;

import com.malafi.payments.malafi_payments.common.BaseEntity;
import com.malafi.payments.malafi_payments.routing.RoutingStrategy;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RoutingStrategy routingStrategy = RoutingStrategy.BALANCED;

    public Merchant(String name, String apiKey) {
        this(name, apiKey, RoutingStrategy.BALANCED);
    }

    public Merchant(String name, String apiKey, RoutingStrategy routingStrategy) {
        this.name = name;
        this.apiKey = apiKey;
        this.routingStrategy = routingStrategy == null ? RoutingStrategy.BALANCED : routingStrategy;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeRoutingStrategy(RoutingStrategy routingStrategy) {
        this.routingStrategy = routingStrategy == null ? RoutingStrategy.BALANCED : routingStrategy;
    }

    public void activate() {
        this.status = MerchantStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = MerchantStatus.INACTIVE;
    }
}
