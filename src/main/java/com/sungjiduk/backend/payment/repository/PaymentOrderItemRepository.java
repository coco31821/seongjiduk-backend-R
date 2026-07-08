package com.sungjiduk.backend.payment.repository;

import com.sungjiduk.backend.payment.entity.PaymentOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentOrderItemRepository extends JpaRepository<PaymentOrderItem, Long> {

    List<PaymentOrderItem> findByPaymentOrderIdOrderByIdAsc(Long paymentOrderId);
}
