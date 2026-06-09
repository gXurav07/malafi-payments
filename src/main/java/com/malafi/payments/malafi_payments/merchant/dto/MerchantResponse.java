package com.malafi.payments.malafi_payments.merchant.dto;

import com.malafi.payments.malafi_payments.merchant.Merchant;
import com.malafi.payments.malafi_payments.merchant.MerchantStatus;
import com.malafi.payments.malafi_payments.routing.RoutingStrategy;

import java.time.Instant;

public record MerchantResponse(
        Long id,
        String name,
        String apiKey,
        MerchantStatus status,
        RoutingStrategy routingStrategy,
        Instant createdAt,
        Instant modifiedAt
) {
    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getName(),
                merchant.getApiKey(),
                merchant.getStatus(),
                merchant.getRoutingStrategy(),
                merchant.getCreatedAt(),
                merchant.getModifiedAt()
        );
    }
}
