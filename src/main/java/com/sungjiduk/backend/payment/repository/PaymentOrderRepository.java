package com.sungjiduk.backend.payment.repository;

import com.sungjiduk.backend.payment.entity.PaymentOrder;
import com.sungjiduk.backend.payment.constants.PaymentOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderId(String orderId);

    List<PaymentOrder> findByUserIdOrderByIdDesc(Long userId);

    List<PaymentOrder> findByTripPlanIdAndStatusOrderByIdDesc(Long tripPlanId, PaymentOrderStatus status);
}
