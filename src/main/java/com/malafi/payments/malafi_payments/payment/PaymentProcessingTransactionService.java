package com.malafi.payments.malafi_payments.payment;

import com.malafi.payments.malafi_payments.payment.dto.PaymentResponse;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttempt;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttemptRepository;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderRequest;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderResult;
import com.malafi.payments.malafi_payments.routing.RoutingDecision;
import com.malafi.payments.malafi_payments.routing.RoutingDecisionRepository;
import com.malafi.payments.malafi_payments.routing.RoutingEngine;
import com.malafi.payments.malafi_payments.routing.RoutingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentProcessingTransactionService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final RoutingDecisionRepository routingDecisionRepository;
    private final RoutingEngine routingEngine;

    @Transactional
    public PaymentProcessingStart startProcessing(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only CREATED payments can be confirmed");
        }

        payment.markProcessing();

        RoutingResult routingResult = routingEngine.route(payment.getMerchant().getRoutingStrategy());

        RoutingDecision routingDecision = new RoutingDecision(payment, routingResult);
        routingDecisionRepository.save(routingDecision);

        PaymentAttempt attempt = new PaymentAttempt(payment, routingResult.selectedPsp().name());
        PaymentAttempt savedAttempt = paymentAttemptRepository.save(attempt);

        return new PaymentProcessingStart(
                routingResult.selectedPsp(),
                new PaymentProviderRequest(
                        savedAttempt.getId(),
                        payment.getId(),
                        payment.getMerchant().getId(),
                        payment.getAmount(),
                        payment.getCurrency()
                )
        );
    }

    @Transactional
    public PaymentResponse completeProcessing(Long attemptId, PaymentProviderResult result) {
        PaymentAttempt attempt = paymentAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment attempt not found"));

        Payment payment = attempt.getPayment();
        applyProviderResult(payment, attempt, result);

        return PaymentResponse.from(payment);
    }

    private void applyProviderResult(Payment payment, PaymentAttempt attempt, PaymentProviderResult result) {
        switch (result.status()) {
            case SUCCESS -> {
                attempt.markSuccess(result.providerReferenceId(), result.latencyMs(), result.cost());
                payment.markSuccess();
            }
            case FAILED -> {
                attempt.markFailed(result.failureReason(), result.latencyMs(), result.cost());
                payment.markFailed();
            }
            case TIMEOUT -> {
                attempt.markTimeout(result.failureReason(), result.latencyMs(), result.cost());
                payment.markFailed();
            }
        }
    }
}
