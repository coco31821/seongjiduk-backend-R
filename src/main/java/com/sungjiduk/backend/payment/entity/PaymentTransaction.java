package com.sungjiduk.backend.payment.entity;

import com.sungjiduk.backend.common.BaseEntity;
import com.sungjiduk.backend.payment.constants.PaymentTransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Toss Payments 승인/실패 기록.
 * PaymentOrder는 성지덕 주문 상태, PaymentTransaction은 PG 거래 결과를 보관한다.
 */
@Entity
@Table(name = "payment_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_order_id", nullable = false)
    private PaymentOrder paymentOrder;

    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    @Column(length = 50)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentTransactionStatus status;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "fail_code", length = 100)
    private String failCode;

    @Column(name = "fail_message", length = 500)
    private String failMessage;

    @Lob
    @Column(name = "raw_payload")
    private String rawPayload;

    @Builder
    private PaymentTransaction(String paymentKey, String method, PaymentTransactionStatus status,
                               Long amount, String currency, String receiptUrl,
                               LocalDateTime approvedAt, String failCode, String failMessage,
                               String rawPayload) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.status = status;
        this.amount = amount;
        this.currency = currency == null ? "KRW" : currency;
        this.receiptUrl = receiptUrl;
        this.approvedAt = approvedAt;
        this.failCode = failCode;
        this.failMessage = failMessage;
        this.rawPayload = rawPayload;
    }

    public static PaymentTransaction succeeded(String paymentKey, String method, Long amount,
                                               String currency, String receiptUrl,
                                               LocalDateTime approvedAt, String rawPayload) {
        return PaymentTransaction.builder()
                .paymentKey(paymentKey)
                .method(method)
                .status(PaymentTransactionStatus.DONE)
                .amount(amount)
                .currency(currency)
                .receiptUrl(receiptUrl)
                .approvedAt(approvedAt)
                .rawPayload(rawPayload)
                .build();
    }

    public static PaymentTransaction failed(String paymentKey, Long amount, String currency,
                                            String failCode, String failMessage, String rawPayload) {
        return PaymentTransaction.builder()
                .paymentKey(paymentKey)
                .status(PaymentTransactionStatus.FAILED)
                .amount(amount)
                .currency(currency)
                .failCode(failCode)
                .failMessage(failMessage)
                .rawPayload(rawPayload)
                .build();
    }

    void assignOrder(PaymentOrder paymentOrder) {
        this.paymentOrder = paymentOrder;
    }
}
