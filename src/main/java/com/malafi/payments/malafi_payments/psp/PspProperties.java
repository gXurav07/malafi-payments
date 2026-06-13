package com.malafi.payments.malafi_payments.psp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "malafi.psp")
public class PspProperties {

    private Map<PspName, Provider> providers = new EnumMap<>(PspName.class);

    public Provider provider(PspName pspName) {
        Provider provider = providers.get(pspName);
        if (provider == null) {
            throw new IllegalStateException("PSP configuration not found for " + pspName);
        }
        return provider;
    }

    @Getter
    @Setter
    public static class Provider {
        private boolean enabled = true;
        private int successRate;
        private int failureRate;
        private int timeoutRate;
        private int normalDelayMinMs;
        private int normalDelayMaxMs;
        private int timeoutDelayMinMs;
        private int timeoutDelayMaxMs;
        private int costBps;
        private List<String> capabilities = List.of();
    }
}
