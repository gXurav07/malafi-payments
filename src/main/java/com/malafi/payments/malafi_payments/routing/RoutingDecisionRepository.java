package com.malafi.payments.malafi_payments.routing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutingDecisionRepository extends JpaRepository<RoutingDecision, Long> {

    List<RoutingDecision> findByPaymentIdOrderByCreatedAtAsc(Long paymentId);
}
