package com.malafi.payments.malafi_payments.payment;

import com.malafi.payments.malafi_payments.payment.dto.CreatePaymentRequest;
import com.malafi.payments.malafi_payments.payment.dto.PaymentResponse;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttemptService;
import com.malafi.payments.malafi_payments.paymentattempt.dto.PaymentAttemptResponse;
import com.malafi.payments.malafi_payments.routing.RoutingDecisionService;
import com.malafi.payments.malafi_payments.routing.dto.RoutingDecisionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentAttemptService paymentAttemptService;
    private final RoutingDecisionService routingDecisionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(
            @PathVariable Long paymentId) {
        return paymentService.getPayment(paymentId);
    }

    @PostMapping("/{paymentId}/confirm")
    public PaymentResponse confirmPayment(
            @PathVariable Long paymentId) {
        return paymentService.confirmPayment(paymentId);
    }

    @GetMapping("/{paymentId}/attempts")
    public List<PaymentAttemptResponse> getPaymentAttempts(
            @PathVariable Long paymentId) {
        return paymentAttemptService.getAttemptsForPayment(paymentId);
    }

    @GetMapping("/{paymentId}/routing-decisions")
    public List<RoutingDecisionResponse> getPaymentRoutingDecisions(
            @PathVariable Long paymentId) {
        return routingDecisionService.getRoutingDecisionsForPayment(paymentId);
    }
}
