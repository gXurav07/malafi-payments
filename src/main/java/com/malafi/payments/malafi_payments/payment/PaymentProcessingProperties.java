package com.malafi.payments.malafi_payments.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "malafi.payment-processing")
public class PaymentProcessingProperties {

    private int maxTotalAttempts = 3;
}
