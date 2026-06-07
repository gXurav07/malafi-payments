package com.malafi.payments.malafi_payments.payment;

import com.malafi.payments.malafi_payments.merchant.MerchantService;
import com.malafi.payments.malafi_payments.merchant.dto.CreateMerchantRequest;
import com.malafi.payments.malafi_payments.merchant.dto.MerchantResponse;
import com.malafi.payments.malafi_payments.payment.dto.CreatePaymentRequest;
import com.malafi.payments.malafi_payments.payment.dto.PaymentResponse;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttempt;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttemptRepository;
import com.malafi.payments.malafi_payments.paymentattempt.PaymentAttemptStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class PaymentPhase1FlowTest {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Test
    void createsMerchantCreatesPaymentAndConfirmsThroughMockPsp() {
        MerchantResponse merchant = merchantService.createMerchant(
                new CreateMerchantRequest("Phase 1 Test Merchant")
        );

        PaymentResponse createdPayment = paymentService.createPayment(
                new CreatePaymentRequest(
                        merchant.id(),
                        new BigDecimal("1000.00"),
                        Currency.INR
                )
        );

        assertNotNull(createdPayment.paymentId());
        assertEquals(merchant.id(), createdPayment.merchantId());
        assertEquals(PaymentStatus.CREATED, createdPayment.status());
        assertEquals(Currency.INR, createdPayment.currency());

        PaymentResponse confirmedPayment = paymentService.confirmPayment(createdPayment.paymentId());

        assertEquals(createdPayment.paymentId(), confirmedPayment.paymentId());
        assertTrue(
                confirmedPayment.status() == PaymentStatus.SUCCESS
                        || confirmedPayment.status() == PaymentStatus.FAILED
        );

        List<PaymentAttempt> attempts = paymentAttemptRepository
                .findByPaymentIdOrderByCreatedAtAsc(createdPayment.paymentId());

        assertEquals(1, attempts.size());

        PaymentAttempt attempt = attempts.getFirst();
        assertEquals(createdPayment.paymentId(), attempt.getPayment().getId());
        assertEquals("CASHFREE_MOCK", attempt.getPspName());
        assertTrue(
                attempt.getStatus() == PaymentAttemptStatus.SUCCESS
                        || attempt.getStatus() == PaymentAttemptStatus.FAILED
                        || attempt.getStatus() == PaymentAttemptStatus.TIMEOUT
        );

        if (attempt.getStatus() == PaymentAttemptStatus.SUCCESS) {
            assertEquals(PaymentStatus.SUCCESS, confirmedPayment.status());
            assertNotNull(attempt.getProviderReferenceId());
        } else {
            assertEquals(PaymentStatus.FAILED, confirmedPayment.status());
            assertFalse(attempt.getFailureReason().isBlank());
        }
    }
}
