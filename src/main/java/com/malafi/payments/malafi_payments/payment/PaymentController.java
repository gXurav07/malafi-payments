package com.malafi.payments.malafi_payments.payment;

import com.malafi.payments.malafi_payments.payment.dto.CreatePaymentRequest;
import com.malafi.payments.malafi_payments.payment.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

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
}
