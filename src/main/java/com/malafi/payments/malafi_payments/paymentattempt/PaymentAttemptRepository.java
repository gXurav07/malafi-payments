package com.malafi.payments.malafi_payments.paymentattempt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    List<PaymentAttempt> findByPaymentIdOrderByCreatedAtAsc(Long paymentId);

    boolean existsByPaymentIdAndStatus(Long paymentId, PaymentAttemptStatus status);
}
