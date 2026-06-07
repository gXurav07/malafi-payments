package com.malafi.payments.malafi_payments.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMerchantRequest(
        @NotBlank(message = "Merchant name is required")
        @Size(max = 120, message = "Merchant name must be at most 120 characters")
        String name
) {
}
