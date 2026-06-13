package com.malafi.payments.malafi_payments.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE payments
            SET status = 'PROCESSING',
                version = version + 1,
                modified_at = CURRENT_TIMESTAMP
            WHERE id = :paymentId
              AND status = 'CREATED'
            """, nativeQuery = true)
    int markCreatedPaymentProcessing(@Param("paymentId") Long paymentId);
}
