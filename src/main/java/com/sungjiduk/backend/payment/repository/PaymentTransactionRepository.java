package com.sungjiduk.backend.payment.repository;

import com.sungjiduk.backend.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByPaymentKey(String paymentKey);

    List<PaymentTransaction> findByPaymentOrderIdOrderByIdDesc(Long paymentOrderId);
}
