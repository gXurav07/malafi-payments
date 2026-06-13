package com.malafi.payments.malafi_payments.payment;

import com.malafi.payments.malafi_payments.merchant.MerchantService;
import com.malafi.payments.malafi_payments.merchant.dto.CreateMerchantRequest;
import com.malafi.payments.malafi_payments.merchant.dto.MerchantResponse;
import com.malafi.payments.malafi_payments.payment.dto.CreatePaymentRequest;
import com.malafi.payments.malafi_payments.payment.dto.PaymentResponse;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttempt;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttemptRepository;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttemptStatus;
import com.malafi.payments.malafi_payments.psp.PspName;
import com.malafi.payments.malafi_payments.psp.PspProperties;
import com.malafi.payments.malafi_payments.routing.RoutingStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class PaymentSimulationPersistenceTest {

    private static final int MERCHANT_COUNT = 8;
    private static final int PAYMENT_COUNT = 300;
    private static final int PARALLELISM = 13;
    private static final String SIMULATION_MERCHANT_NAME_PATTERN = "Simulation Merchant %";
    private static final Random RANDOM = new Random(20260610L);

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PspProperties pspProperties;

    @Test
    void simulatesPaymentsAndPersistsResultsForInspection() throws Exception {
        SimulationRun simulationRun = runSimulation();

        Map<Long, Metrics> merchantMetrics = new LinkedHashMap<>();
        Map<PspName, Metrics> pspMetrics = new EnumMap<>(PspName.class);

        for (MerchantResponse merchant : simulationRun.merchants()) {
            merchantMetrics.put(merchant.id(), new Metrics());
        }

        for (SimulationResult result : simulationRun.results()) {
            merchantMetrics.get(result.merchantId()).record(result.success(), result.latencyMs(), result.cost());
            pspMetrics.computeIfAbsent(result.pspName(), ignored -> new Metrics()).record(result.success(), result.latencyMs(), result.cost());
        }

        assertEquals(PAYMENT_COUNT, merchantMetrics.values().stream().mapToInt(Metrics::total).sum());
        printResults(simulationRun.merchants(), merchantMetrics, pspMetrics);
    }

    @Test
    void simulatesPaymentsAndPrintsTabularReports() throws Exception {
        SimulationRun simulationRun = runSimulation();

        assertEquals(PAYMENT_COUNT, simulationRun.results().size());
        printMerchantReport(simulationRun);
        printStrategyPspReport(simulationRun);
    }

    private SimulationRun runSimulation() throws InterruptedException, ExecutionException {
        cleanupPreviousSimulationData();

        List<MerchantResponse> merchants = createMerchants();
        List<SimulationRequest> simulationRequests = createSimulationRequests(merchants);
        List<SimulationResult> simulationResults = runPaymentsInParallel(simulationRequests);

        return new SimulationRun(merchants, simulationResults);
    }


    private List<MerchantResponse> createMerchants() {
        RoutingStrategy[] strategies = RoutingStrategy.values();
        List<MerchantResponse> merchants = new ArrayList<>();

        for (int index = 0; index < MERCHANT_COUNT; index++) {
            RoutingStrategy strategy = strategies[index % strategies.length];
            merchants.add(merchantService.createMerchant(new CreateMerchantRequest(
                    "Simulation Merchant " + (index + 1) + " - " + strategy,
                    strategy
            )));
        }

        return merchants;
    }

    private List<SimulationRequest> createSimulationRequests(List<MerchantResponse> merchants) {
        List<SimulationRequest> requests = new ArrayList<>();

        for (int index = 0; index < PAYMENT_COUNT; index++) {
            MerchantResponse merchant = merchants.get(RANDOM.nextInt(merchants.size()));
            requests.add(new SimulationRequest(merchant.id(), randomAmount()));
        }

        return requests;
    }

    private List<SimulationResult> runPaymentsInParallel(List<SimulationRequest> requests)
            throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(PARALLELISM);
        try {
            List<Future<SimulationResult>> futures = new ArrayList<>();
            for (SimulationRequest request : requests) {
                futures.add(executorService.submit(() -> processSimulationRequest(request)));
            }

            List<SimulationResult> results = new ArrayList<>();
            for (Future<SimulationResult> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executorService.shutdown();
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        }
    }

    private SimulationResult processSimulationRequest(SimulationRequest request) {
        PaymentResponse createdPayment = paymentService.createPayment(new CreatePaymentRequest(
                request.merchantId(),
                request.amount(),
                Currency.INR
        ));

        PaymentResponse confirmedPayment = paymentService.confirmPayment(createdPayment.paymentId());
        List<PaymentAttempt> attempts = paymentAttemptRepository
                .findByPaymentIdOrderByCreatedAtAsc(createdPayment.paymentId());
        PaymentAttempt finalAttempt = attempts.getLast();

        boolean success = confirmedPayment.status() == PaymentStatus.SUCCESS
                && finalAttempt.getStatus() == PaymentAttemptStatus.SUCCESS;

        return new SimulationResult(
                request.merchantId(),
                request.amount(),
                success,
                attempts.stream()
                        .map(attempt -> new AttemptResult(
                                PspName.valueOf(attempt.getPspName()),
                                attempt.getStatus(),
                                attempt.getLatencyMs(),
                                attempt.getCost()
                        ))
                        .toList()
        );
    }

    private BigDecimal randomAmount() {
        long amountInPaise = 10_000L + RANDOM.nextLong(240_000L);
        return BigDecimal.valueOf(amountInPaise, 2);
    }

    private void printResults(
            List<MerchantResponse> merchants,
            Map<Long, Metrics> merchantMetrics,
            Map<PspName, Metrics> pspMetrics) {
        System.out.println();
        System.out.println("=== Persistent Phase 2 Payment Simulation ===");
        System.out.println("Generated merchants: " + MERCHANT_COUNT);
        System.out.println("Generated payments: " + PAYMENT_COUNT);
        System.out.println("Database rows are intentionally persisted for manual inspection.");

        System.out.println();
        System.out.println("Merchant metrics");
        System.out.printf("%-12s %-28s %-20s %8s %10s %15s %12s%n",
                "merchantId", "name", "strategy", "payments", "success%", "avgLatencyMs", "avgCost");
        for (MerchantResponse merchant : merchants) {
            Metrics metrics = merchantMetrics.get(merchant.id());
            System.out.printf("%-12d %-28s %-20s %8d %10.2f %15.2f %12s%n",
                    merchant.id(),
                    truncate(merchant.name(), 28),
                    merchant.routingStrategy(),
                    metrics.total(),
                    metrics.successRate(),
                    metrics.averageLatencyMs(),
                    metrics.averageCost());
        }

        System.out.println();
        System.out.println("PSP metrics");
        System.out.printf("%-18s %8s %10s %15s %12s%n",
                "psp", "payments", "success%", "avgLatencyMs", "avgCost");
        for (Map.Entry<PspName, Metrics> entry : pspMetrics.entrySet()) {
            Metrics metrics = entry.getValue();
            System.out.printf("%-18s %8d %10.2f %15.2f %12s%n",
                    entry.getKey(),
                    metrics.total(),
                    metrics.successRate(),
                    metrics.averageLatencyMs(),
                    metrics.averageCost());
        }
        System.out.println();
    }

    private void printMerchantReport(SimulationRun simulationRun) {
        Map<Long, MerchantReportMetrics> metricsByMerchant = new LinkedHashMap<>();
        for (MerchantResponse merchant : simulationRun.merchants()) {
            metricsByMerchant.put(merchant.id(), new MerchantReportMetrics());
        }

        for (SimulationResult result : simulationRun.results()) {
            metricsByMerchant.get(result.merchantId()).record(result);
        }

        int[] widths = {12, 20, 8, 14, 14, 12, 21};
        printSeparator(widths);
        printRow(widths, "Merchant", "Strategy", "Payments", "Avg Amount INR", "Success Rate %", "Cost Bps", "Fallback Recoveries");
        printSeparator(widths);

        int index = 1;
        for (MerchantResponse merchant : simulationRun.merchants()) {
            MerchantReportMetrics metrics = metricsByMerchant.get(merchant.id());
            printRow(
                    widths,
                    "merchant_" + index,
                    merchant.routingStrategy().name(),
                    String.valueOf(metrics.payments()),
                    formatDecimal(metrics.averageAmount()),
                    formatDouble(metrics.successRate()),
                    formatDecimal(metrics.effectiveCostBps()),
                    String.valueOf(metrics.fallbackRecoveries())
            );
            index++;
        }

        printSeparator(widths);
    }

    private void printStrategyPspReport(SimulationRun simulationRun) {
        Map<Long, RoutingStrategy> strategyByMerchant = new LinkedHashMap<>();
        for (MerchantResponse merchant : simulationRun.merchants()) {
            strategyByMerchant.put(merchant.id(), merchant.routingStrategy());
        }

        Map<StrategyPspKey, PspReportMetrics> metricsByStrategyAndPsp = new LinkedHashMap<>();
        for (SimulationResult result : simulationRun.results()) {
            RoutingStrategy strategy = strategyByMerchant.get(result.merchantId());
            for (AttemptResult attempt : result.attempts()) {
                StrategyPspKey key = new StrategyPspKey(strategy, attempt.pspName());
                metricsByStrategyAndPsp
                        .computeIfAbsent(key, ignored -> new PspReportMetrics())
                        .record(attempt);
            }
        }

        int[] widths = {12, 10, 8, 8, 8, 10, 14, 8};
        printSeparator(widths);
        printRow(widths, "Strategy", "PSP", "Selected", "Success", "Failed", "Success %", "Avg Latency ms", "Cost Bps");
        printSeparator(widths);

        for (RoutingStrategy strategy : RoutingStrategy.values()) {
            for (PspName pspName : PspName.values()) {
                StrategyPspKey key = new StrategyPspKey(strategy, pspName);
                PspReportMetrics metrics = metricsByStrategyAndPsp.get(key);
                if (metrics == null || metrics.selected() == 0) {
                    continue;
                }

                printRow(
                        widths,
                        strategy.name(),
                        displayPspName(pspName),
                        String.valueOf(metrics.selected()),
                        String.valueOf(metrics.success()),
                        String.valueOf(metrics.failed()),
                        formatDouble(metrics.successRate()),
                        String.valueOf(Math.round(metrics.averageLatencyMs())),
                        String.valueOf(pspProperties.provider(pspName).getCostBps())
                );
            }
        }

        printSeparator(widths);
    }

    private void printSeparator(int[] widths) {
        StringBuilder builder = new StringBuilder();
        for (int width : widths) {
            builder.append("+").append("-".repeat(width + 2));
        }
        builder.append("+");
        System.out.println(builder);
    }

    private void printRow(int[] widths, String... values) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < widths.length; index++) {
            builder.append("| ")
                    .append(pad(values[index], widths[index]))
                    .append(" ");
        }
        builder.append("|");
        System.out.println(builder);
    }

    private String pad(String value, int width) {
        String truncated = value.length() > width ? value.substring(0, width - 3) + "..." : value;
        return truncated + " ".repeat(width - truncated.length());
    }

    private String displayPspName(PspName pspName) {
        return pspName.name().replace("_MOCK", "");
    }

    private String formatDouble(double value) {
        return String.format("%.2f", value);
    }

    private String formatDecimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private record SimulationRequest(
            Long merchantId,
            BigDecimal amount
    ) {
    }

    private record SimulationRun(
            List<MerchantResponse> merchants,
            List<SimulationResult> results
    ) {
    }

    private record SimulationResult(
            Long merchantId,
            BigDecimal amount,
            boolean success,
            List<AttemptResult> attempts
    ) {
        PspName pspName() {
            return attempts.getLast().pspName();
        }

        long latencyMs() {
            return attempts.getLast().latencyMs();
        }

        BigDecimal cost() {
            return attempts.stream()
                    .map(AttemptResult::cost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        boolean fallbackRecovered() {
            return success && attempts.size() > 1;
        }
    }

    private record AttemptResult(
            PspName pspName,
            PaymentAttemptStatus status,
            long latencyMs,
            BigDecimal cost
    ) {
        boolean success() {
            return status == PaymentAttemptStatus.SUCCESS;
        }
    }

    private record StrategyPspKey(
            RoutingStrategy strategy,
            PspName pspName
    ) {
    }

    private static class Metrics {
        private int total;
        private int success;
        private long totalLatencyMs;
        private BigDecimal totalCost = BigDecimal.ZERO;

        void record(boolean successful, long latencyMs, BigDecimal cost) {
            total++;
            if (successful) {
                success++;
            }
            totalLatencyMs += latencyMs;
            totalCost = totalCost.add(cost);
        }

        int total() {
            return total;
        }

        double successRate() {
            if (total == 0) {
                return 0.0;
            }
            return (success * 100.0) / total;
        }

        double averageLatencyMs() {
            if (total == 0) {
                return 0.0;
            }
            return totalLatencyMs / (double) total;
        }

        BigDecimal averageCost() {
            if (total == 0) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            return totalCost.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }
    }

    private static class MerchantReportMetrics {
        private int payments;
        private int success;
        private int fallbackRecoveries;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private BigDecimal totalCost = BigDecimal.ZERO;

        void record(SimulationResult result) {
            payments++;
            if (result.success()) {
                success++;
            }
            if (result.fallbackRecovered()) {
                fallbackRecoveries++;
            }
            totalAmount = totalAmount.add(result.amount());
            totalCost = totalCost.add(result.cost());
        }

        int payments() {
            return payments;
        }

        double successRate() {
            if (payments == 0) {
                return 0.0;
            }
            return (success * 100.0) / payments;
        }

        BigDecimal averageAmount() {
            if (payments == 0) {
                return BigDecimal.ZERO;
            }
            return totalAmount.divide(BigDecimal.valueOf(payments), 2, RoundingMode.HALF_UP);
        }

        BigDecimal effectiveCostBps() {
            if (totalAmount.signum() == 0) {
                return BigDecimal.ZERO;
            }
            return totalCost.multiply(BigDecimal.valueOf(10_000))
                    .divide(totalAmount, 2, RoundingMode.HALF_UP);
        }

        int fallbackRecoveries() {
            return fallbackRecoveries;
        }
    }

    private static class PspReportMetrics {
        private int selected;
        private int success;
        private long totalLatencyMs;

        void record(AttemptResult attempt) {
            selected++;
            if (attempt.success()) {
                success++;
            }
            totalLatencyMs += attempt.latencyMs();
        }

        int selected() {
            return selected;
        }

        int success() {
            return success;
        }

        int failed() {
            return selected - success;
        }

        double successRate() {
            if (selected == 0) {
                return 0.0;
            }
            return (success * 100.0) / selected;
        }

        double averageLatencyMs() {
            if (selected == 0) {
                return 0.0;
            }
            return totalLatencyMs / (double) selected;
        }
    }

    private void cleanupPreviousSimulationData() {
        int routingDecisionsDeleted = jdbcTemplate.update("""
                DELETE FROM routing_decisions rd
                USING payments p, merchants m
                WHERE rd.payment_id = p.id
                  AND p.merchant_id = m.id
                  AND m.name LIKE ?
                """, SIMULATION_MERCHANT_NAME_PATTERN);

        int attemptsDeleted = jdbcTemplate.update("""
                DELETE FROM payment_attempts pa
                USING payments p, merchants m
                WHERE pa.payment_id = p.id
                  AND p.merchant_id = m.id
                  AND m.name LIKE ?
                """, SIMULATION_MERCHANT_NAME_PATTERN);

        int paymentsDeleted = jdbcTemplate.update("""
                DELETE FROM payments p
                USING merchants m
                WHERE p.merchant_id = m.id
                  AND m.name LIKE ?
                """, SIMULATION_MERCHANT_NAME_PATTERN);

        int merchantsDeleted = jdbcTemplate.update("""
                DELETE FROM merchants
                WHERE name LIKE ?
                """, SIMULATION_MERCHANT_NAME_PATTERN);

        System.out.println();
        System.out.println("=== Previous Simulation Cleanup ===");
        System.out.println("Deleted routing decisions: " + routingDecisionsDeleted);
        System.out.println("Deleted payment attempts: " + attemptsDeleted);
        System.out.println("Deleted payments: " + paymentsDeleted);
        System.out.println("Deleted merchants: " + merchantsDeleted);
    }
}
