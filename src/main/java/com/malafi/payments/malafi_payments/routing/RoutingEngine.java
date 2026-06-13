package com.malafi.payments.malafi_payments.routing;

import com.malafi.payments.malafi_payments.psp.PspName;
import com.malafi.payments.malafi_payments.psp.PspProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoutingEngine {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PspProperties pspProperties;

    public RoutingResult route(RoutingStrategy strategy) {
        RoutingStrategy resolvedStrategy = strategy == null ? RoutingStrategy.BALANCED : strategy;
        List<Map.Entry<PspName, PspProperties.Provider>> enabledProviders = pspProperties.getProviders()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().isEnabled())
                .toList();

        if (enabledProviders.isEmpty()) {
            throw new IllegalStateException("No enabled PSP providers configured");
        }

        int configuredMaxCostBps = enabledProviders.stream()
                .mapToInt(entry -> entry.getValue().getCostBps())
                .max()
                .orElse(1);
        int maxCostBps = Math.max(configuredMaxCostBps, 1);
        int maxLatencyMs = enabledProviders.stream()
                .mapToInt(entry -> averageNormalLatencyMs(entry.getValue()))
                .max()
                .orElse(1);

        List<RoutingCandidate> candidates = enabledProviders.stream()
                .map(entry -> toCandidate(entry.getKey(), entry.getValue(), resolvedStrategy, maxCostBps, maxLatencyMs))
                .sorted(Comparator.comparing(RoutingCandidate::score).reversed())
                .toList();

        RoutingCandidate selected = candidates.getFirst();
        return new RoutingResult(
                resolvedStrategy,
                selected.pspName(),
                selected.score(),
                selected.reason(),
                toCandidateSummary(candidates),
                candidates.stream().map(RoutingCandidate::pspName).toList()
        );
    }

    private RoutingCandidate toCandidate(
            PspName pspName,
            PspProperties.Provider provider,
            RoutingStrategy strategy,
            int maxCostBps,
            int maxLatencyMs) {
        BigDecimal score = score(provider, strategy, maxCostBps, maxLatencyMs);
        return new RoutingCandidate(pspName, score, reason(pspName, provider, strategy, score));
    }

    private BigDecimal score(
            PspProperties.Provider provider,
            RoutingStrategy strategy,
            int maxCostBps,
            int maxLatencyMs) {
        BigDecimal successScore = BigDecimal.valueOf(provider.getSuccessRate()).divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP);
        BigDecimal costScore = BigDecimal.ONE.subtract(BigDecimal.valueOf(provider.getCostBps())
                .divide(BigDecimal.valueOf(maxCostBps), 4, RoundingMode.HALF_UP));
        BigDecimal latencyScore = BigDecimal.ONE.subtract(BigDecimal.valueOf(averageNormalLatencyMs(provider))
                .divide(BigDecimal.valueOf(maxLatencyMs), 4, RoundingMode.HALF_UP));

        BigDecimal score = switch (strategy) {
            case LOWEST_COST -> costScore;
            case BEST_SUCCESS_RATE -> successScore;
            case LOWEST_LATENCY -> latencyScore;
            case BALANCED -> successScore.multiply(new BigDecimal("0.55"))
                    .add(latencyScore.multiply(new BigDecimal("0.25")))
                    .add(costScore.multiply(new BigDecimal("0.20")));
        };

        return score.setScale(4, RoundingMode.HALF_UP);
    }

    private int averageNormalLatencyMs(PspProperties.Provider provider) {
        return (provider.getNormalDelayMinMs() + provider.getNormalDelayMaxMs()) / 2;
    }

    private String reason(
            PspName pspName,
            PspProperties.Provider provider,
            RoutingStrategy strategy,
            BigDecimal score) {
        return "Selected " + pspName
                + " using " + strategy
                + " score=" + score
                + ", successRate=" + provider.getSuccessRate()
                + ", avgLatencyMs=" + averageNormalLatencyMs(provider)
                + ", costBps=" + provider.getCostBps();
    }

    private String toCandidateSummary(List<RoutingCandidate> candidates) {
        return candidates.stream()
                .map(candidate -> candidate.pspName() + ":" + candidate.score())
                .collect(Collectors.joining(","));
    }
}
