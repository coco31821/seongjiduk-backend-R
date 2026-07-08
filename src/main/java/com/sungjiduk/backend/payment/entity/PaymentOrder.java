package com.sungjiduk.backend.payment.entity;

import com.sungjiduk.backend.common.BaseEntity;
import com.sungjiduk.backend.payment.constants.PaymentOrderStatus;
import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 성지덕 기준 결제 주문.
 * Toss orderId와 사용자에게 청구할 최종 금액은 이 엔티티를 기준으로 검증한다.
 */
@Entity
@Table(name = "payment_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_plan_id", nullable = false)
    private TripPlan tripPlan;

    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(name = "order_name", nullable = false, length = 200)
    private String orderName;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentOrderStatus status;

    @OneToMany(mappedBy = "paymentOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "paymentOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentTransaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "paymentOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierBooking> supplierBookings = new ArrayList<>();

    @Builder
    private PaymentOrder(User user, TripPlan tripPlan, String orderId, String orderName,
                         Long totalAmount, String currency, PaymentOrderStatus status) {
        this.user = user;
        this.tripPlan = tripPlan;
        this.orderId = orderId;
        this.orderName = orderName;
        this.totalAmount = totalAmount;
        this.currency = currency == null ? "KRW" : currency;
        this.status = status == null ? PaymentOrderStatus.READY : status;
    }

    public void addItem(PaymentOrderItem item) {
        items.add(item);
        item.assignOrder(this);
    }

    public void addTransaction(PaymentTransaction transaction) {
        transactions.add(transaction);
        transaction.assignOrder(this);
    }

    public void addSupplierBooking(SupplierBooking supplierBooking) {
        supplierBookings.add(supplierBooking);
        supplierBooking.assignOrder(this);
    }

    public boolean isOwnedBy(Long userId) {
        return user != null && user.getId() != null && user.getId().equals(userId);
    }

    public void markPaymentDone() {
        this.status = PaymentOrderStatus.PAYMENT_DONE;
    }

    public void markBookingRequested() {
        this.status = PaymentOrderStatus.BOOKING_REQUESTED;
    }

    public void markBooked() {
        this.status = PaymentOrderStatus.BOOKED;
    }

    public void markFailed() {
        this.status = PaymentOrderStatus.FAILED;
    }

    public void markCanceled() {
        this.status = PaymentOrderStatus.CANCELED;
    }

    public void markRefunded() {
        this.status = PaymentOrderStatus.REFUNDED;
    }
}
