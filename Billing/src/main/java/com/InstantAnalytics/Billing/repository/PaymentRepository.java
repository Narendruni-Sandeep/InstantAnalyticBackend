package com.InstantAnalytics.Billing.repository;

import com.InstantAnalytics.Billing.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserId(Long userId);
    
    Optional<Payment> findByTransactionId(String transactionId);

}
