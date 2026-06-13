package com.malafi.payments.malafi_payments.routing;

import com.malafi.payments.malafi_payments.payment.PaymentRepository;
import com.malafi.payments.malafi_payments.routing.dto.RoutingDecisionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutingDecisionService {

    private final PaymentRepository paymentRepository;
    private final RoutingDecisionRepository routingDecisionRepository;

    @Transactional(readOnly = true)
    public List<RoutingDecisionResponse> getRoutingDecisionsForPayment(Long paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }

        return routingDecisionRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId)
                .stream()
                .map(RoutingDecisionResponse::from)
                .toList();
    }
}
