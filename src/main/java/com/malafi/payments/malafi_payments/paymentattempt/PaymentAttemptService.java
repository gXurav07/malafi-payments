package com.malafi.payments.malafi_payments.paymentattempt;

import com.malafi.payments.malafi_payments.payment.PaymentRepository;
import com.malafi.payments.malafi_payments.paymentattempt.dto.PaymentAttemptResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentAttemptService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;

    @Transactional(readOnly = true)
    public List<PaymentAttemptResponse> getAttemptsForPayment(Long paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }

        return paymentAttemptRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId)
                .stream()
                .map(PaymentAttemptResponse::from)
                .toList();
    }
}
