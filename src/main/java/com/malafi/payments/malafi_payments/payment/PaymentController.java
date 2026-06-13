package com.malafi.payments.malafi_payments.payment;

import com.malafi.payments.malafi_payments.idempotency.IdempotencyResult;
import com.malafi.payments.malafi_payments.idempotency.IdempotencyService;
import com.malafi.payments.malafi_payments.payment.dto.CreatePaymentRequest;
import com.malafi.payments.malafi_payments.payment.dto.PaymentResponse;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttemptService;
import com.malafi.payments.malafi_payments.paymentattempt.dto.PaymentAttemptResponse;
import com.malafi.payments.malafi_payments.routing.RoutingDecisionService;
import com.malafi.payments.malafi_payments.routing.dto.RoutingDecisionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentAttemptService paymentAttemptService;
    private final RoutingDecisionService routingDecisionService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        IdempotencyResult<PaymentResponse> result = idempotencyService.executePaymentResponse(
                idempotencyKey,
                idempotencyService.fingerprint("CREATE_PAYMENT", request),
                HttpStatus.CREATED,
                () -> paymentService.createPayment(request)
        );

        return ResponseEntity.status(result.status()).body(result.response());
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(
            @PathVariable Long paymentId) {
        return paymentService.getPayment(paymentId);
    }

    @PostMapping("/{paymentId}/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable Long paymentId) {
        IdempotencyResult<PaymentResponse> result = idempotencyService.executePaymentResponse(
                idempotencyKey,
                idempotencyService.fingerprint("CONFIRM_PAYMENT", paymentId),
                HttpStatus.OK,
                () -> paymentService.confirmPayment(paymentId)
        );

        return ResponseEntity.status(result.status()).body(result.response());
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
