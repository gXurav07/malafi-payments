package com.malafi.payments.malafi_payments.payment;

import com.malafi.payments.malafi_payments.payment.dto.PaymentResponse;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttempt;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttemptRepository;
import com.malafi.payments.malafi_payments.psp.PaymentProviderAdapter;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderRequest;
import com.malafi.payments.malafi_payments.psp.dto.PaymentProviderResult;
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
    private final PaymentProviderAdapter paymentProviderAdapter;

    @Transactional
    public PaymentProviderRequest startProcessing(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only CREATED payments can be confirmed");
        }

        payment.markProcessing();

        PaymentAttempt attempt = new PaymentAttempt(payment, paymentProviderAdapter.providerName().name());
        PaymentAttempt savedAttempt = paymentAttemptRepository.save(attempt);

        return new PaymentProviderRequest(
                savedAttempt.getId(),
                payment.getId(),
                payment.getMerchant().getId(),
                payment.getAmount(),
                payment.getCurrency()
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
                attempt.markSuccess(result.providerReferenceId());
                payment.markSuccess();
            }
            case FAILED -> {
                attempt.markFailed(result.failureReason());
                payment.markFailed();
            }
            case TIMEOUT -> {
                attempt.markTimeout(result.failureReason());
                payment.markFailed();
            }
        }
    }
}
