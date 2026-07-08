package com.sungjiduk.backend.payment.repository;

import com.sungjiduk.backend.payment.entity.SupplierBooking;
import com.sungjiduk.backend.payment.constants.SupplierBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierBookingRepository extends JpaRepository<SupplierBooking, Long> {

    List<SupplierBooking> findByPaymentOrderIdOrderByIdAsc(Long paymentOrderId);

    List<SupplierBooking> findByPaymentOrderIdAndStatusOrderByIdAsc(
            Long paymentOrderId,
            SupplierBookingStatus status
    );
}
