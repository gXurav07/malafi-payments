package com.malafi.payments.malafi_payments.payment;

import com.malafi.payments.malafi_payments.merchant.Merchant;
import com.malafi.payments.malafi_payments.merchant.MerchantRepository;
import com.malafi.payments.malafi_payments.merchant.MerchantStatus;
import com.malafi.payments.malafi_payments.payment.dto.CreatePaymentRequest;
import com.malafi.payments.malafi_payments.payment.dto.PaymentResponse;
import com.malafi.payments.malafi_payments.psp.PaymentProviderRegistry;
import com.malafi.payments.malafi_payments.psp.PspProperties;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderResult;
import com.malafi.payments.malafi_payments.psp.PspName;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final PaymentProviderRegistry paymentProviderRegistry;
    private final PspProperties pspProperties;
    private final PaymentProcessingProperties paymentProcessingProperties;
    private final PaymentProcessingTransactionService paymentProcessingTransactionService;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Merchant merchant = merchantRepository.findById(request.merchantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Merchant is not active");
        }

        Payment payment = new Payment(merchant, request.amount(), request.currency());
        Payment savedPayment = paymentRepository.save(payment);

        return PaymentResponse.from(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        return PaymentResponse.from(payment);
    }

    public PaymentResponse confirmPayment(Long paymentId) {
        PaymentProcessingStart current =
                paymentProcessingTransactionService.startProcessing(paymentId);

        int attemptsUsed = 0;

        while (current != null && attemptsUsed < paymentProcessingProperties.getMaxTotalAttempts()) {
            attemptsUsed++;

            PaymentProviderResult result = processWithMetrics(current);
            boolean shouldRetry = shouldRetry(result, current, attemptsUsed);

            PaymentResponse response = paymentProcessingTransactionService.completeProcessing(
                    current.providerRequest().attemptId(),
                    result,
                    !shouldRetry
            );

            if (!shouldRetry || response.status() == PaymentStatus.SUCCESS) {
                return response;
            }

            current = createNextAttempt(current);
        }

        throw new IllegalStateException("Payment processing ended without final response");
    }

    private boolean shouldRetry(
            PaymentProviderResult result,
            PaymentProcessingStart current,
            int attemptsUsed) {
        return result.isRetryable()
                && !current.remainingPsps().isEmpty()
                && attemptsUsed < paymentProcessingProperties.getMaxTotalAttempts();
    }

    private PaymentProcessingStart createNextAttempt(PaymentProcessingStart current) {
        List<PspName> remaining = current.remainingPsps();

        if (remaining.isEmpty()) {
            throw new IllegalStateException("No PSP available for retry");
        }

        return paymentProcessingTransactionService.createNextAttempt(
                current.providerRequest().paymentId(),
                remaining.getFirst(),
                remaining.subList(1, remaining.size())
        );
    }


    private PaymentProviderResult processWithMetrics(PaymentProcessingStart processingStart) {
        long startTimeNanos = System.nanoTime();
        PaymentProviderResult result = paymentProviderRegistry
                .get(processingStart.selectedPsp())
                .process(processingStart.providerRequest());
        long latencyMs = (System.nanoTime() - startTimeNanos) / 1_000_000;

        return result.withMetrics(latencyMs, calculateCost(processingStart));
    }

    private BigDecimal calculateCost(PaymentProcessingStart processingStart) {
        int costBps = pspProperties.provider(processingStart.selectedPsp()).getCostBps();
        return processingStart.providerRequest()
                .amount()
                .multiply(BigDecimal.valueOf(costBps))
                .divide(BigDecimal.valueOf(10_000), 2, RoundingMode.HALF_UP);
    }
}
